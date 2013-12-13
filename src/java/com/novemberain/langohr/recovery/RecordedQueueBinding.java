package com.novemberain.langohr.recovery;

import com.novemberain.langohr.Channel;

import java.io.IOException;

public class RecordedQueueBinding extends RecordedBinding implements RecoverableEntity {
  public RecordedQueueBinding(Channel channel) {
    super(channel);
  }

  public Object recover() throws IOException {
    return this.channel.getDelegate().queueBind(this.getDestination(), this.getSource(), this.routingKey, this.arguments);
  }
}
