package com.novemberain.langohr.queue;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;
import java.util.Map;

public class PurgeOk extends PersistentMapLike implements AMQP.Queue.PurgeOk {
  private final AMQP.Queue.PurgeOk method;

  public PurgeOk(AMQP.Queue.PurgeOk method) {
    this.method = method;

    this.map = mapFrom(method);

  }

  public static IPersistentMap mapFrom(AMQP.Queue.PurgeOk method) {
    Map<Keyword, Object> m = new HashMap<Keyword, Object>();
    m.put(RT.keyword(null, "message-count"), method.getMessageCount());
    m.put(RT.keyword(null, "message_count"), method.getMessageCount());

    return PersistentHashMap.create(m);
  }

  public int getMessageCount() {
    return method.getMessageCount();
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
