// Copyright (c) 2011-2020 Michael S. Klishin
//
// The use and distribution terms for this software are covered by the
// Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
// which can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any fashion, you are agreeing to be bound by
// the terms of this license.
// You must not remove this notice, or any other, from this software.

package com.novemberain.langohr;

import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * Alternative {@link com.rabbitmq.client.Connection} implementation that wraps
 * {@link com.rabbitmq.client.impl.AMQConnection} and adds automatic connection
 * recovery capability to it.
 */
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
  private static final Keyword CONNECTION_NAME_KEYWORD = Keyword.intern(null, "connection-name");
  private final IPersistentMap options;

  private com.rabbitmq.client.Connection delegate;

  //
  // recovery
  //

  private boolean automaticallyRecover;
  private boolean automaticallyRecoverTopology;

  private static IPersistentMap buildDefaultOptions() {
    Map<Keyword, Boolean> m = new HashMap<Keyword, Boolean>();

    return PersistentHashMap.create(m);
  }

  private final ConnectionFactory cf;

  public Connection(ConnectionFactory cf) {
    this(cf, DEFAULT_OPTIONS);
  }

  public Connection(ConnectionFactory cf, IPersistentMap options) {
    this.cf = cf;
    this.options = options;

    Long l = (Long) options.valAt(NETWORK_RECOVERY_DELAY_KEYWORD, DEFAULT_NETWORK_RECOVERY_DELAY);
    cf.setNetworkRecoveryInterval(l.intValue());

    this.automaticallyRecover = Util.isTruthy(options.valAt(AUTOMATICALLY_RECOVER_KEYWORD, true));
    this.automaticallyRecoverTopology = Util.isTruthy(options.valAt(AUTOMATICALLY_RECOVER_TOPOLOGY_KEYWORD, true));

    cf.setAutomaticRecoveryEnabled(this.automaticallyRecover);
    cf.setTopologyRecoveryEnabled(this.automaticallyRecoverTopology);
  }

  @SuppressWarnings("unused")
  public Connection init() throws IOException, TimeoutException {
    return init(new Address[]{});
  }

  @SuppressWarnings("unused")
  public Connection init(Address[] addresses) throws IOException, TimeoutException {
    ExecutorService es = (ExecutorService) this.options.valAt(EXECUTOR_KEYWORD);

    String cn = (String) this.options.valAt(CONNECTION_NAME_KEYWORD);

    if (addresses.length > 0) {
      if(cn != null) {
        this.delegate = cf.newConnection(es, addresses, cn);
      } else {
        this.delegate = cf.newConnection(es, addresses);
      }
    } else {
      if(cn != null) {
        this.delegate = cf.newConnection(es, cn);
      } else {
        this.delegate = cf.newConnection(es);
      }
    }

    return this;
  }

  public boolean automaticRecoveryEnabled() {
    return automaticallyRecover;
  }

  public boolean automaticTopologyRecoveryEnabled() {
    return automaticallyRecoverTopology;
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
  }

  public BlockedListener addBlockedListener(BlockedCallback blockedCallback, UnblockedCallback unblockedCallback) {
    return delegate.addBlockedListener(blockedCallback, unblockedCallback);
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
   * Create a new channel, using an internally allocated channel number.
   *
   * @return a new channel descriptor, or null if none is available
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public Channel createChannel() throws IOException {
    return delegate.createChannel();
  }

  /**
   * Create a new channel, using the specified channel number if possible.
   *
   * @param channelNumber the channel number to allocate
   * @return a new channel descriptor, or null if this channel number is already in use
   * @throws java.io.IOException if an I/O problem is encountered
   */
  public Channel createChannel(int channelNumber) throws IOException {
    return delegate.createChannel(channelNumber);
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
   * Returns client-provided connection name, if any. Note that the value
   * returned does not uniquely identify a connection and cannot be used
   * as a connection identifier in HTTP API requests.
   *
   * @return client-provided connection name, if any
   * @see ConnectionFactory#newConnection(Address[], String)
   * @see ConnectionFactory#newConnection(ExecutorService, Address[], String)
   */
  public String getClientProvidedName() {
    return delegate.getClientProvidedName();
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

  public void setId(String id) {
    delegate.setId(id);
  }

  public String getId() {
    return delegate.getId();
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
  }

  public boolean removeBlockedListener(BlockedListener listener) {
    return delegate.removeBlockedListener(listener);
  }

  public void clearBlockedListeners() {
    delegate.clearBlockedListeners();
  }

  public ExceptionHandler getExceptionHandler() {
    return delegate.getExceptionHandler();
  }

  public void addRecoveryListener(RecoveryListener listener) {
    ((AutorecoveringConnection) this.delegate).addRecoveryListener(listener);
  }

  public void removeRecoveryListener(RecoveryListener listener) {
    ((AutorecoveringConnection) this.delegate).removeRecoveryListener(listener);
  }

  @Override
  public String toString() {
    return this.delegate.toString();
  }
}
