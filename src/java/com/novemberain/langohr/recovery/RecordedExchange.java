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
