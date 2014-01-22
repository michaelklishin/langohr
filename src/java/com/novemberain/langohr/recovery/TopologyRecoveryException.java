package com.novemberain.langohr.recovery;

public class TopologyRecoveryException extends Exception {
  public TopologyRecoveryException(String message) {
    super(message);
  }

  public TopologyRecoveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
