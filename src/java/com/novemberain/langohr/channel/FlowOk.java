package com.novemberain.langohr.channel;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;

public class FlowOk extends PersistentMapLike implements AMQP.Channel.FlowOk {
  private final AMQP.Channel.FlowOk method;

  public FlowOk(AMQP.Channel.FlowOk method) {
    this.method = method;

    this.map = mapFrom(method);

  }

  public static IPersistentMap mapFrom(AMQP.Channel.FlowOk method) {
    final HashMap m = new HashMap();
    m.put(RT.keyword(null, "active"), method.getActive());
    return PersistentHashMap.create(m);
  }

  public int protocolClassId() {
    return method.protocolClassId();
  }

  public int protocolMethodId() {
    return method.protocolMethodId();
  }

  public String protocolMethodName() {
    return method.protocolMethodName();
  }

  public boolean getActive() {
    return method.getActive();
  }
}
