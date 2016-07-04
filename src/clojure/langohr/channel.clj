;; This source code is dual-licensed under the Apache License, version
;; 2.0, and the Eclipse Public License, version 1.0.
;;
;; The APL v2.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;; ----------------------------------------------------------------------------------
;;
;; The EPL v1.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;; All rights reserved.
;;
;; This program and the accompanying materials are made available under the terms of
;; the Eclipse Public License Version 1.0,
;; which accompanies this distribution and is available at
;; http://www.eclipse.org/legal/epl-v10.html.
;; ----------------------------------------------------------------------------------

(ns langohr.channel
  "Operations on channels.

   Relevant guides:

   * http://clojurerabbitmq.info/articles/connecting.html"
  (:import [com.rabbitmq.client Connection Channel]
           com.novemberain.langohr.channel.FlowOk
           com.rabbitmq.client.impl.recovery.AutorecoveringChannel))

(defn ^Channel as-non-recovering-channel
  [^Channel ch]
  (if (instance? AutorecoveringChannel ch)
    (.getDelegate ^AutorecoveringChannel ch)
    ch))


;;
;; API
;;

(defn ^Channel open
  "Opens a new channel on given connection using channel.open AMQP method.

   Returns nil if channel cannot be open because the number of open channel
   would surpass negotiated channel_max connection setting."
  ([^Connection connection]
     (.createChannel connection))
  ([^Connection connection id]
     (.createChannel connection id)))


(defn close
  "Closes given channel using channel.close AMQP method"
  ([^Channel channel]
     (.close channel))
  ([^Channel channel ^long code ^String message]
     (.close channel code message)))


(defn open?
  "Checks if channel is open. Consider using langohr.core/open? instead."
  [^Channel channel]
  (.isOpen channel))
(def closed? (complement open?))
