package com.novemberain.langohr.queue;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;

public class UnbindOk extends PersistentMapLike implements AMQP.Queue.UnbindOk {
  private final AMQP.Queue.UnbindOk method;

  public UnbindOk(AMQP.Queue.UnbindOk method) {
    this.method = method;
    this.map = mapFrom(method);
  }

  public static IPersistentMap mapFrom(AMQP.Queue.UnbindOk method) {
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
