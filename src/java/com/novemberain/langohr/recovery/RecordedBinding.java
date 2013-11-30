package com.novemberain.langohr.recovery;

import com.novemberain.langohr.Channel;

import java.util.Map;

@SuppressWarnings("unused")
public abstract class RecordedBinding extends RecordedEntity implements RecoverableEntity {
  protected String source;
  protected String destination;
  protected String routingKey;
  protected Map<String, Object> arguments;

  public RecordedBinding(Channel channel) {
    super(channel);
  }

  public RecordedBinding source(String value) {
    this.source = value;
    return this;
  }

  public RecordedBinding destination(String value) {
    this.destination = value;
    return this;
  }

  public RecordedBinding routingKey(String value) {
    this.routingKey = value;
    return this;
  }

  public RecordedBinding arguments(Map<String, Object> value) {
    this.arguments = value;
    return this;
  }

  public String getSource() {
    return source;
  }

  public String getDestination() {
    return destination;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

}
