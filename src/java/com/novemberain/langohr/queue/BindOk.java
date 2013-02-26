package com.novemberain.langohr.queue;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;

public class BindOk extends PersistentMapLike implements AMQP.Queue.BindOk {
  private final AMQP.Queue.BindOk method;

  public BindOk(AMQP.Queue.BindOk method) {
    this.method = method;
    this.map = mapFrom(method);
  }

  public static IPersistentMap mapFrom(AMQP.Queue.BindOk method) {
    return PersistentHashMap.create(new HashMap());
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
}
