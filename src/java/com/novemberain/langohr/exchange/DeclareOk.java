package com.novemberain.langohr.exchange;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;

public class DeclareOk extends PersistentMapLike implements AMQP.Exchange.DeclareOk {
  private final AMQP.Exchange.DeclareOk method;

  public DeclareOk(AMQP.Exchange.DeclareOk method) {
    this.method = method;

    this.map = mapFrom(method);
  }

  public static IPersistentMap mapFrom(AMQP.Exchange.DeclareOk method) {
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
