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

import java.io.IOException;
import java.util.Map;

public class RecordedExchange extends RecordedNamedEntity implements RecoverableEntity {
  private boolean durable;
  private boolean autoDelete;
  private Map<String, Object> arguments;
  private String type;

  public RecordedExchange(Channel channel, String name) {
    super(channel, name);
  }

  public Object recover() throws IOException {
    return this.channel.getDelegate().
        exchangeDeclare(this.name,
                        this.type,
                        this.durable,
                        this.autoDelete,
                        this.arguments);
  }

  public RecordedExchange durable(boolean value) {
    this.durable = value;
    return this;
  }

  public RecordedExchange autoDelete(boolean value) {
    this.autoDelete = value;
    return this;
  }

  public RecordedExchange type(String value) {
    this.type = value;
    return this;
  }

  public RecordedExchange arguments(Map<String, Object> value) {
    this.arguments = value;
    return this;
  }
}
