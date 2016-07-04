// Copyright (c) 2011-2016 Michael S. Klishin
//
// The use and distribution terms for this software are covered by the
// Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
// which can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any fashion, you are agreeing to be bound by
// the terms of this license.
// You must not remove this notice, or any other, from this software.

package com.novemberain.langohr.tx;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.novemberain.langohr.PersistentMapLike;
import com.rabbitmq.client.AMQP;

import java.util.HashMap;

public class RollbackOk extends PersistentMapLike implements AMQP.Tx.RollbackOk {
  private final AMQP.Tx.RollbackOk method;

  public RollbackOk(AMQP.Tx.RollbackOk method) {
    this.method = method;

    this.map = mapFrom(method);

  }

  public static IPersistentMap mapFrom(AMQP.Tx.RollbackOk method) {
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
