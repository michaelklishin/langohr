package com.novemberain.langohr;

import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import com.novemberain.langohr.recovery.*;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class Connection implements com.rabbitmq.client.Connection, Recoverable {
  private static final IPersistentMap DEFAULT_OPTIONS = buildDefaultOptions();
  // :automatically-recover
  public static final String AUTOMATICALLY_RECOVER_KEYWORD_NAME = "automatically-recover";
  public static final Keyword AUTOMATICALLY_RECOVER_KEYWORD = Keyword.intern(null, AUTOMATICALLY_RECOVER_KEYWORD_NAME);
  // :automatically-recover-topology
  public static final String AUTOMATICALLY_RECOVER_TOPOLOGY_KEYWORD_NAME = "automatically-recover-topology";
  public static final Keyword AUTOMATICALLY_RECOVER_TOPOLOGY_KEYWORD = Keyword.intern(null, AUTOMATICALLY_RECOVER_TOPOLOGY_KEYWORD_NAME);
  // :network-recovery-delay
  private static final String NETWORK_RECOVERY_DELAY_KEYWORD_NAME = "network-recovery-delay";
  private static final Keyword NETWORK_RECOVERY_DELAY_KEYWORD = Keyword.intern(null, NETWORK_RECOVERY_DELAY_KEYWORD_NAME);
  private static final long DEFAULT_NETWORK_RECOVERY_DELAY = 5000;
  private static final Keyword EXECUTOR_KEYWORD = Keyword.intern(null, "executor");
  private static final long DEFAULT_RECONNECTION_PERIOD = 5000;
  private final IPersistentMap options;
  private final List<ShutdownListener> shutdownHooks;
  private final List<IFn> recoveryHooks;
  private com.rabbitmq.client.Connection delegate;
  private Map<Integer, Channel> channels;
  private final Collection<BlockedListener> blockedListeners = new CopyOnWriteArrayList<BlockedListener>();
  private long networkRecoveryDelay;

  // Records topology changes
  private final Map<String, RecordedQueue> recordedQueues = new ConcurrentHashMap<String, RecordedQueue>();
  private final List<RecordedBinding> recordedBindings = new ArrayList<RecordedBinding>();
  private Map<String, RecordedExchange> recordedExchanges = new ConcurrentHashMap<String, RecordedExchange>();
  private final Map<String, RecordedConsumer> consumers = new ConcurrentHashMap<String, RecordedConsumer>();

  private static IPersistentMap buildDefaultOptions() {
    Map<Keyword, Boolean> m = new HashMap<Keyword, Boolean>();
    m.put(AUTOMATICALLY_RECOVER_KEYWORD, true);
    m.put(AUTOMATICALLY_RECOVER_TOPOLOGY_KEYWORD, true);

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
    this.networkRecoveryDelay = (Long) options.valAt(NETWORK_RECOVERY_DELAY_KEYWORD, DEFAULT_NETWORK_RECOVERY_DELAY);
  }

  @SuppressWarnings("unused")
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
    /*
    Shutdown listener that kicks off automatic connection recovery
    if it is enabled.
   */
    ShutdownListener automaticRecoveryListener = new ShutdownListener() {
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

  synchronized private void beginAutomaticRecovery() throws InterruptedException, IOException {
    try {
      Thread.sleep(networkRecoveryDelay);
      this.recoverConnection();
      this.recoverShutdownHooks();
      this.recoverChannels();
      if(automaticTopologyRecoveryEnabled()) {
        this.recoverEntites();
        this.recoverConsumers();
      }

      this.runRecoveryHooks();
      this.runChannelRecoveryHooks();
    } catch (Throwable t) {
      System.err.println("Caught an exception during connection recovery!");
      t.printStackTrace(System.err);
    }

  }

  private void runRecoveryHooks() {
    for (IFn f : recoveryHooks) {
      f.invoke(this);
    }
  }

  private void runChannelRecoveryHooks() {
    for (Channel ch : this.channels.values()) {
      ch.runRecoveryHooks();
    }
  }

  private void recoverChannels() throws IOException {
    for (Channel ch : this.channels.values()) {
      try {
        ch.automaticallyRecover(this, this.delegate);
      } catch (Throwable t) {
        System.err.println("Caught an exception when recovering channel " + ch.getChannelNumber());
        t.printStackTrace(System.err);
      }
    }
  }

  private void recoverShutdownHooks() {
    for (ShutdownListener sh : this.shutdownHooks) {
      this.delegate.addShutdownListener(sh);
    }
  }

  private void recoverConnection() throws IOException, InterruptedException {
    boolean recovering = true;
    while (recovering) {
      try {
        ExecutorService es = (ExecutorService) this.options.valAt(EXECUTOR_KEYWORD);
        this.delegate = this.cf.newConnection(es);
        recovering = false;
      } catch (ConnectException ce) {
        System.err.println("Failed to reconnect: " + ce.getMessage());
        // TODO: exponential back-off
        Thread.sleep(DEFAULT_RECONNECTION_PERIOD);
      }
    }
  }

  public boolean automaticRecoveryEnabled() {
    return this.options.containsKey(AUTOMATICALLY_RECOVER_KEYWORD);
  }

  public boolean automaticTopologyRecoveryEnabled() {
    return this.options.containsKey(AUTOMATICALLY_RECOVER_TOPOLOGY_KEYWORD);
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
    if (delegateChannel == null) {
      return null;
    } else {
      this.registerChannel(channel);
      return channel;
    }
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

  @SuppressWarnings("unused")
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
    com.rabbitmq.client.Channel ch = delegate.createChannel();
    if (ch == null) {
      return null;
    } else {
      return this.wrapChannel(ch);
    }
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
    delegate.addBlockedListener(listener);
    blockedListeners.add(listener);
  }

  public boolean removeBlockedListener(BlockedListener listener) {
    boolean result = blockedListeners.remove(listener);
    delegate.removeBlockedListener(listener);
    return result;
  }

  public void clearBlockedListeners() {
    blockedListeners.clear();
  }


  //
  // Recovery
  //

  public void recoverEntites() {
    // The recovery sequence is the following:
    //
    // 1. Recover exchanges
    // 2. Recover queues
    // 3. Recover bindings
    // 4. Recover consumers
    recoverExchanges();
    recoverQueues();
    recoverBindings();
  }

  private void recoverExchanges() {
    // recorded exchanges are guaranteed to be
    // non-predefined (we filter out predefined ones
    // in exchangeDeclare). MK.
    for (RecordedExchange x : this.recordedExchanges.values()) {
      try {
        x.recover();
      } catch (Exception e) {
        System.err.println("Caught an exception while recovering exchange " + x.getName());
        e.printStackTrace(System.err);
      }
    }
  }

  private void recoverQueues() {
    for (Map.Entry<String, RecordedQueue> entry : this.recordedQueues.entrySet()) {
      String oldName = entry.getKey();
      RecordedQueue q = entry.getValue();
      try {
        q.recover();
        String newName = q.getName();
        // make sure server-named queues are re-added with
        // their new names. MK.
        synchronized (this.recordedQueues) {
          deleteRecordedQueue(oldName);
          this.recordedQueues.put(newName, q);
          this.propagateQueueNameChangeToBindings(oldName, newName);
          this.propagateQueueNameChangeToConsumers(oldName, newName);
        }
      } catch (Exception e) {
        System.err.println("Caught an exception while recovering queue " + oldName);
        e.printStackTrace(System.err);
      }
    }
  }

  private void propagateQueueNameChangeToBindings(String oldName, String newName) {
    for (RecordedBinding b : this.recordedBindings) {
      if (b.getDestination().equals(oldName)) {
        b.setDestination(newName);
      }
    }
  }

  private void propagateQueueNameChangeToConsumers(String oldName, String newName) {
    for (RecordedConsumer c : this.consumers.values()) {
      if (c.getQueue().equals(oldName)) {
        c.setQueue(newName);
      }
    }
  }

  public void recoverBindings() {
    for (RecordedBinding b : this.recordedBindings) {
      try {
        b.recover();
      } catch (Exception e) {
        System.err.println("Caught an exception while recovering binding between " + b.getSource() + " and " + b.getDestination());
        e.printStackTrace(System.err);
      }
    }
  }

  public void recoverConsumers() {
    for (Map.Entry<String, RecordedConsumer> entry : this.consumers.entrySet()) {
      String tag = entry.getKey();
      RecordedConsumer consumer = entry.getValue();

      try {
        String newTag = (String) consumer.recover();
        // make sure server-generated tags are re-added. MK.
        synchronized (this.consumers) {
          this.consumers.remove(tag);
          this.consumers.put(newTag, consumer);
        }
      } catch (Exception e) {
        System.err.println("Caught an exception while recovering consumer " + tag);
        e.printStackTrace(System.err);
      }
    }
  }


  public synchronized void recordQueueBinding(Channel ch, String queue, String exchange, String routingKey, Map<String, Object> arguments) {
    RecordedBinding binding = new RecordedQueueBinding(ch).
        source(exchange).
        destination(queue).
        routingKey(routingKey).
        arguments(arguments);
    if (!this.recordedBindings.contains(binding)) {
      this.recordedBindings.add(binding);
    }
  }

  public synchronized boolean deleteRecordedQueueBinding(Channel ch, String queue, String exchange, String routingKey, Map<String, Object> arguments) {
    RecordedBinding b = new RecordedQueueBinding(ch).
        source(exchange).
        destination(queue).
        routingKey(routingKey).
        arguments(arguments);
    return this.recordedBindings.remove(b);
  }

  public synchronized void recordExchangeBinding(Channel ch, String destination, String source, String routingKey, Map<String, Object> arguments) {
    RecordedBinding binding = new RecordedExchangeBinding(ch).
        source(source).
        destination(destination).
        routingKey(routingKey).
        arguments(arguments);
    this.recordedBindings.add(binding);
  }

  public synchronized boolean deleteRecordedExchangeBinding(Channel ch, String destination, String source, String routingKey, Map<String, Object> arguments) {
    RecordedBinding b = new RecordedExchangeBinding(ch).
        source(source).
        destination(destination).
        routingKey(routingKey).
        arguments(arguments);
    return this.recordedBindings.remove(b);
  }

  public void recordQueue(AMQP.Queue.DeclareOk ok, RecordedQueue q) {
    this.recordedQueues.put(ok.getQueue(), q);
  }

  public void deleteRecordedQueue(String queue) {
    this.recordedQueues.remove(queue);
  }

  public void recordExchange(String exchange, RecordedExchange x) {
    this.recordedExchanges.put(exchange, x);
  }

  public void deleteRecordedExchange(String exchange) {
    this.recordedExchanges.remove(exchange);
  }

  public void recordConsumer(String result, RecordedConsumer consumer) {
    this.consumers.put(result, consumer);
  }

  public void deleteRecordedConsumer(String consumerTag) {
    this.consumers.remove(consumerTag);
  }

  public void registerChannel(Channel channel) {
    this.channels.put(channel.getChannelNumber(), channel);
  }

  public void unregisterChannel(Channel channel) {
    this.channels.remove(channel.getChannelNumber());
  }
}
