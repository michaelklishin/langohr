// Copyright (c) 2011-2016 Michael S. Klishin
//
// The use and distribution terms for this software are covered by the
// Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
// which can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any fashion, you are agreeing to be bound by
// the terms of this license.
// You must not remove this notice, or any other, from this software.

package com.novemberain.langohr;

import clojure.lang.IMapEntry;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ISeq;

import java.util.Iterator;

public abstract class PersistentMapLike implements IPersistentMap {
  protected IPersistentMap map;

  public Object valAt(Object o) {
    return this.map.valAt(o);
  }

  public Object valAt(Object o, Object o2) {
    return this.map.valAt(o, o2);
  }

  public IPersistentMap assoc(Object o, Object o2) {
    return map.assoc(o, o2);
  }

  public IPersistentMap assocEx(Object o, Object o2) {
    return map.assocEx(o, o2);
  }

  public IPersistentMap without(Object o) {
    return map.without(o);
  }

  public Iterator iterator() {
    return map.iterator();
  }

  public boolean containsKey(Object o) {
    return map.containsKey(o);
  }

  public IMapEntry entryAt(Object o) {
    return map.entryAt(o);
  }

  public IPersistentCollection cons(Object o) {
    return map.cons(o);
  }

  public int count() {
    return map.count();
  }

  public IPersistentCollection empty() {
    return map.empty();
  }

  public boolean equiv(Object o) {
    return map.equiv(o);
  }

  public ISeq seq() {
    return map.seq();
  }
}
