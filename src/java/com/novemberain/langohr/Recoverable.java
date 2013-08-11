package com.novemberain.langohr;

import clojure.lang.IFn;

/**
 * Provides a way to register (network, AMQP 0-9-1) connection recovery
 * callbacks.
 */
public interface Recoverable {
  /**
   * Registers a connection recovery callback.
   *
   * @param f Callback function
   */
  public void onRecovery(IFn f);
}
