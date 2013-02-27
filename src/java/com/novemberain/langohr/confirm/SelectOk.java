package com.novemberain.langohr.confirm;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;

public class SelectOk extends PersistentMapLike implements AMQP.Confirm.SelectOk {
  private final AMQP.Confirm.SelectOk method;

  public SelectOk(AMQP.Confirm.SelectOk method) {
    this.method = method;

    this.map = mapFrom(method);

  }

  public static IPersistentMap mapFrom(AMQP.Confirm.SelectOk method) {
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
