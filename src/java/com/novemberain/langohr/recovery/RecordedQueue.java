// Copyright (c) 2011-2014 Michael S. Klishin
//
// The use and distribution terms for this software are covered by the
// Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
// which can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any fashion, you are agreeing to be bound by
// the terms of this license.
// You must not remove this notice, or any other, from this software.

package com.novemberain.langohr.recovery;

import com.novemberain.langohr.Channel;
import com.rabbitmq.client.AMQP;

import java.io.IOException;
import java.util.Map;

public class RecordedQueue extends RecordedNamedEntity implements RecoverableEntity {
  public static final String EMPTY_STRING = "";
  private boolean durable;
  private boolean autoDelete;
  private Map<String, Object> arguments;
  private boolean exclusive;
  private boolean serverNamed;

  public RecordedQueue(Channel channel, String name) {
    super(channel, name);
  }

  public RecordedQueue exclusive(boolean value) {
    this.exclusive = value;
    return this;
  }

  public RecordedQueue serverNamed(boolean value) {
    this.serverNamed = value;
    return this;
  }

  public boolean isServerNamed() {
    return this.serverNamed;
  }

  public Object recover() throws IOException {
    AMQP.Queue.DeclareOk ok = this.channel.getDelegate().
        queueDeclare(this.getNameToUseForRecovery(),
                     this.durable,
                     this.exclusive,
                     this.autoDelete,
                     this.arguments);
    this.name = ok.getQueue();

    return ok;
  }

  public String getNameToUseForRecovery() {
    if(isServerNamed()) {
      return EMPTY_STRING;
    } else {
      return this.name;
    }
  }

  public RecordedQueue durable(boolean value) {
    this.durable = value;
    return this;
  }

  public RecordedQueue autoDelete(boolean value) {
    this.autoDelete = value;
    return this;
  }

  public RecordedQueue arguments(Map<String, Object> value) {
    this.arguments = value;
    return this;
  }
}
