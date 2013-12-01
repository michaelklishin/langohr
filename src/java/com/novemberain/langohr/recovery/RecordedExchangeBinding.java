package com.novemberain.langohr.recovery;

import com.novemberain.langohr.Channel;

import java.io.IOException;

public class RecordedExchangeBinding extends RecordedBinding implements RecoverableEntity {
  public RecordedExchangeBinding(Channel channel) {
    super(channel);
  }

  public Object recover() throws IOException {
    return this.channel.exchangeBind(this.source, this.destination, this.routingKey, this.arguments);
  }
}
