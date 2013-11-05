package com.novemberain.langohr;

import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class Connection implements com.rabbitmq.client.Connection, Recoverable {
  private static final IPersistentMap DEFAULT_OPTIONS = buildDefaultOptions();
  public static final String AUTOMATICALLY_RECOVER_KEYWORD_NAME = "automatically-recover";
  public static final Keyword AUTOMATICALLY_RECOVER_KEYWORD = Keyword.intern(null, AUTOMATICALLY_RECOVER_KEYWORD_NAME);
  private static final long DEFAULT_NETWORK_RECOVERY_PERIOD = 5000;
  private static final Keyword EXECUTOR_KEYWORD = Keyword.intern(null, "executor");
  private final IPersistentMap options;
  private final List<ShutdownListener> shutdownHooks;
  private final List<IFn> recoveryHooks;
  private com.rabbitmq.client.Connection delegate;
  /**
   * Shutdown listener that kicks off automatic connection recovery
   * if it is enabled.
   */
  private ShutdownListener automaticRecoveryListener;
  private Map<Integer, Channel> channels;
  private final Collection<BlockedListener> blockedListeners = new CopyOnWriteArrayList<BlockedListener>();

  private static IPersistentMap buildDefaultOptions() {
    Map<Keyword, Boolean> m = new HashMap<Keyword, Boolean>();
    m.put(AUTOMATICALLY_RECOVER_KEYWORD, true);

    return PersistentHashMap.create(m);
  }

  private final ConnectionFactory cf;

  public Connection(ConnectionFactory cf) {
    this(cf, DEFAULT_OPTIONS);
  }

  public Connection(ConnectionFactory cf, IPersistentMap options) {
    this.cf = cf;
    this.options = options;

    this.channels = new ConcurrentHashMap<Integer, Channel>();
    this.shutdownHooks = new ArrayList<ShutdownListener>();
    // network failure recovery hooks
    this.recoveryHooks = new ArrayList<IFn>();
  }

  public Connection init() throws IOException {
    ExecutorService es = (ExecutorService) this.options.valAt(EXECUTOR_KEYWORD);
    this.delegate = cf.newConnection(es);

    if (this.automaticRecoveryEnabled()) {
      this.addAutomaticRecoveryHook();
    }

    return this;
  }

  private void addAutomaticRecoveryHook() {
    final Connection c = this;
    automaticRecoveryListener = new ShutdownListener() {
      public void shutdownCompleted(ShutdownSignalException cause) {
        try {
          if (!cause.isInitiatedByApplication()) {
            c.beginAutomaticRecovery();
          }
        } catch (InterruptedException e) {
          // no-op, we cannot really do anything useful here,
          // doing nothing will prevent automatic recovery
          // from continuing. MK.
        } catch (IOException e) {
          // no-op, see above
          // TODO: exponential backoff on how long we wait
        }
      }
    };

    synchronized (this) {
      this.shutdownHooks.add(automaticRecoveryListener);
      this.delegate.addShutdownListener(automaticRecoveryListener);
    }
  }

  private void beginAutomaticRecovery() throws InterruptedException, IOException {
    Thread.sleep(DEFAULT_NETWORK_RECOVERY_PERIOD);

    this.recoverConnection();
    this.recoverShutdownHooks();
    this.recoverChannels();

    for (IFn f : recoveryHooks) {
      f.invoke(this);
    }
    this.runChannelRecoveryHooks();
  }

  private void runChannelRecoveryHooks() {
    Iterator<Map.Entry<Integer, Channel>> it = this.channels.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Channel> e = it.next();
      Channel ch = e.getValue();

      ch.runRecoveryHooks();
    }
  }

  private void recoverChannels() throws IOException {
    Iterator<Map.Entry<Integer, Channel>> it = this.channels.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Channel> e = it.next();
      Channel ch = e.getValue();

      ch.automaticallyRecover(this, this.delegate);
    }
  }

  private void recoverShutdownHooks() {
    for (ShutdownListener sh : this.shutdownHooks) {
      this.delegate.addShutdownListener(sh);
    }
  }

  private void recoverConnection() throws IOException {
    ExecutorService es = (ExecutorService) this.options.valAt(EXECUTOR_KEYWORD);
    this.delegate = this.cf.newConnection(es);
  }

  public boolean automaticRecoveryEnabled() {
    return this.options.containsKey(AUTOMATICALLY_RECOVER_KEYWORD);
  }

  public void onRecovery(IFn f) {
    this.recoveryHooks.add(f);
  }

  /**
   * Abort this connection and all its channels
   * with the {@link com.rabbitmq.client.AMQP#REPLY_SUCCESS} close code
   * and message 'OK'.
   * <p/>
   * Forces the connection to close.
   * Any encountered exceptions in the close operations are silently discarded.
   */
  public void abort() {
    delegate.abort();
  }

  /**
   * Remove shutdown listener for the component.
   *
   * @param listener {@link com.rabbitmq.client.ShutdownListener} to be removed
   */
  public void removeShutdownListener(ShutdownListener listener) {
    delegate.removeShutdownListener(listener);
  }

  /**
   * Get the negotiated maximum channel number. Usable channel
   * numbers range from 1 to this number, inclusive.
   *
   * @return the maximum channel number permitted for this connection.
   */
  public int getChannelMax() {
    return delegate.getChannelMax();
  }

  /**
   * Add shutdown listener.
   * If the component is already closed, handler is fired immediately
   *
   * @param listener {@link com.rabbitmq.client.ShutdownListener} to the component
   */
  public void addShutdownListener(ShutdownListener listener) {
    delegate.addShutdownListener(listener);
    this.shutdownHooks.add(listener);
  }

  /**
   * Close this connection and all its channels.
   * <p/>
   * Waits with the given timeout for all the close operations to complete.
   * When timeout is reached the socket is forced to close.
   *
   * @param closeCode    the close code (See under "Reply Codes" in the AMQP specification)
   * @param closeMessage a message indicating the reason for closing the connection
   * @param timeout      timeout (in milliseconds) for completing all the close-related
   *                     operations, use -1 for infinity
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public void close(int closeCode, String closeMessage, int timeout) throws IOException {
    delegate.close(closeCode, closeMessage, timeout);
  }

  /**
   * Retrieve the port number.
   *
   * @return the port number of the peer we're connected to.
   */
  public int getPort() {
    return delegate.getPort();
  }

  /**
   * Close this connection and all its channels
   * with the {@link com.rabbitmq.client.AMQP#REPLY_SUCCESS} close code
   * and message 'OK'.
   * <p/>
   * This method behaves in a similar way as {@link #close()}, with the only difference
   * that it waits with a provided timeout for all the close operations to
   * complete. When timeout is reached the socket is forced to close.
   *
   * @param timeout timeout (in milliseconds) for completing all the close-related
   *                operations, use -1 for infinity
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public void close(int timeout) throws IOException {
    delegate.close(timeout);
  }

  /**
   * Create a Langohr channel from a RabbitMQ channel and register it.
   *
   * @param delegateChannel A RabbitMQ channel.
   * @return The Langohr channel.
   */
  private Channel wrapChannel(com.rabbitmq.client.Channel delegateChannel) {
    final Channel channel = new Channel(this, delegateChannel);
    this.registerChannel(channel);
    return channel;
  }

  /**
   * Create a new channel, using the specified channel number if possible.
   *
   * @param channelNumber the channel number to allocate
   * @return a new channel descriptor, or null if this channel number is already in use
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public Channel createChannel(int channelNumber) throws IOException {
    return this.wrapChannel(delegate.createChannel(channelNumber));
  }

  private void registerChannel(Channel channel) {
    this.channels.put(channel.getChannelNumber(), channel);
  }

  public void unregisterChannel(Channel channel) {
    this.channels.remove(channel.getChannelNumber());
  }

  /**
   * Abort this connection and all its channels
   * with the {@link com.rabbitmq.client.AMQP#REPLY_SUCCESS} close code
   * and message 'OK'.
   * <p/>
   * This method behaves in a similar way as {@link #abort()}, with the only difference
   * that it waits with a provided timeout for all the close operations to
   * complete. When timeout is reached the socket is forced to close.
   *
   * @param timeout timeout (in milliseconds) for completing all the close-related
   *                operations, use -1 for infinity
   */
  public void abort(int timeout) {
    delegate.abort(timeout);
  }

  public com.rabbitmq.client.Connection getDelegate() {
    return delegate;
  }

  /**
   * Create a new channel, using an internally allocated channel number.
   *
   * @return a new channel descriptor, or null if none is available
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public Channel createChannel() throws IOException {
    return this.wrapChannel(delegate.createChannel());
  }

  /**
   * Abort this connection and all its channels.
   * <p/>
   * Forces the connection to close and waits for all the close operations to complete.
   * Any encountered exceptions in the close operations are silently discarded.
   *
   * @param closeCode    the close code (See under "Reply Codes" in the AMQP specification)
   * @param closeMessage a message indicating the reason for closing the connection
   */
  public void abort(int closeCode, String closeMessage) {
    delegate.abort(closeCode, closeMessage);
  }

  /**
   * Close this connection and all its channels
   * with the {@link com.rabbitmq.client.AMQP#REPLY_SUCCESS} close code
   * and message 'OK'.
   * <p/>
   * Waits for all the close operations to complete.
   *
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public void close() throws IOException {
    delegate.close();
  }

  /**
   * Retrieve the server properties.
   *
   * @return a map of the server properties. This typically includes the product name and version of the server.
   */
  public Map<String, Object> getServerProperties() {
    return delegate.getServerProperties();
  }

  /**
   * Get a copy of the map of client properties sent to the server
   *
   * @return a copy of the map of client properties
   */
  public Map<String, Object> getClientProperties() {
    return delegate.getClientProperties();
  }

  /**
   * Close this connection and all its channels.
   * <p/>
   * Waits for all the close operations to complete.
   *
   * @param closeCode    the close code (See under "Reply Codes" in the AMQP specification)
   * @param closeMessage a message indicating the reason for closing the connection
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public void close(int closeCode, String closeMessage) throws IOException {
    delegate.close(closeCode, closeMessage);
  }

  /**
   * Get the shutdown reason object
   *
   * @return ShutdownSignalException if component is closed, null otherwise
   */
  public ShutdownSignalException getCloseReason() {
    return delegate.getCloseReason();
  }

  /**
   * Retrieve the host.
   *
   * @return the hostname of the peer we're connected to.
   */
  public InetAddress getAddress() {
    return delegate.getAddress();
  }

  /**
   * Get the negotiated heartbeat interval.
   *
   * @return the heartbeat interval, in seconds; zero if none
   */
  public int getHeartbeat() {
    return delegate.getHeartbeat();
  }

  /**
   * Determine whether the component is currently open.
   * Will return false if we are currently closing.
   * Checking this method should be only for information,
   * because of the race conditions - state can change after the call.
   * Instead just execute and try to catch ShutdownSignalException
   * and IOException
   *
   * @return true when component is open, false otherwise
   */
  public boolean isOpen() {
    return delegate.isOpen();
  }

  /**
   * Protected API - notify the listeners attached to the component
   *
   * @see com.rabbitmq.client.ShutdownListener
   */
  public void notifyListeners() {
    delegate.notifyListeners();
  }

  /**
   * Get the negotiated maximum frame size.
   *
   * @return the maximum frame size, in octets; zero if unlimited
   */
  public int getFrameMax() {
    return delegate.getFrameMax();
  }

  /**
   * Abort this connection and all its channels.
   * <p/>
   * Forces the connection to close and waits with the given timeout
   * for all the close operations to complete. When timeout is reached
   * the socket is forced to close.
   * Any encountered exceptions in the close operations are silently discarded.
   *
   * @param closeCode    the close code (See under "Reply Codes" in the AMQP specification)
   * @param closeMessage a message indicating the reason for closing the connection
   * @param timeout      timeout (in milliseconds) for completing all the close-related
   *                     operations, use -1 for infinity
   */
  public void abort(int closeCode, String closeMessage, int timeout) {
    delegate.abort(closeCode, closeMessage, timeout);
  }

  public void addBlockedListener(BlockedListener listener) {
    blockedListeners.add(listener);
  }

  public boolean removeBlockedListener(BlockedListener listener) {
    return blockedListeners.remove(listener);
  }

  public void clearBlockedListeners() {
    blockedListeners.clear();
  }
}
