package com.novemberain.langohr.queue;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;
import java.util.Map;

public class DeclareOk extends PersistentMapLike implements AMQP.Queue.DeclareOk {
  private final AMQP.Queue.DeclareOk method;

  public DeclareOk(AMQP.Queue.DeclareOk method) {
    this.method = method;

    this.map = mapFrom(method);

  }

  public static IPersistentMap mapFrom(AMQP.Queue.DeclareOk method) {
    Map m = new HashMap();
    m.put(RT.keyword(null, "queue"), method.getQueue());
    m.put(RT.keyword(null, "message-count"), method.getMessageCount());
    m.put(RT.keyword(null, "consumer-count"), method.getConsumerCount());
    m.put(RT.keyword(null, "message_count"), method.getMessageCount());
    m.put(RT.keyword(null, "consumer_count"), method.getConsumerCount());

    return PersistentHashMap.create(m);
  }

  public int getConsumerCount() {
    return method.getConsumerCount();
  }

  public int getMessageCount() {
    return method.getMessageCount();
  }

  public String getQueue() {
    return method.getQueue();
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
