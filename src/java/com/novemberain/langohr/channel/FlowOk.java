// Copyright (c) 2011-2016 Michael S. Klishin
//
// The use and distribution terms for this software are covered by the
// Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
// which can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any fashion, you are agreeing to be bound by
// the terms of this license.
// You must not remove this notice, or any other, from this software.

package com.novemberain.langohr.channel;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

public class FlowOk extends PersistentMapLike implements AMQP.Channel.FlowOk {
  private final AMQP.Channel.FlowOk method;

  public FlowOk(AMQP.Channel.FlowOk method) {
    this.method = method;

    this.map = mapFrom(method);

  }

  public static IPersistentMap mapFrom(AMQP.Channel.FlowOk method) {
    final Map<Keyword, Object> m = new HashMap<Keyword, Object>();
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
