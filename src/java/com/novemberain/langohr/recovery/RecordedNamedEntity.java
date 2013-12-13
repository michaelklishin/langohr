package com.novemberain.langohr.recovery;

import com.novemberain.langohr.Channel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RecordedNamedEntity extends RecordedEntity {
  protected String name;

  public RecordedNamedEntity(Channel channel, String name) {
    super(channel);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
