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

(ns langohr.confirm
  "Functions that work with publisher confirms.

   Relevant guides:

   * http://clojurerabbitmq.info/articles/exchanges.html"
  (:import [com.rabbitmq.client ConfirmListener]
           com.novemberain.langohr.confirm.SelectOk
           com.rabbitmq.client.Channel))


;;
;; API
;;


(defn ^ConfirmListener listener
  "Instantiates and returns a new confirmations listener that handles basic.ack and basic.nack method deliveries"
  [^clojure.lang.IFn ack-handler ^clojure.lang.IFn nack-handler]
  (reify ConfirmListener
    (handleAck [this delivery-tag multiple]
      (ack-handler delivery-tag multiple))
    (handleNack [this delivery-tag multiple]
      (nack-handler delivery-tag multiple))))


(defn ^Channel add-listener
  "Adds confirmations listener to given channel"
  [^Channel channel ^ConfirmListener cl]
  (.addConfirmListener channel cl)
  channel)


(defn ^com.novemberain.langohr.confirm.SelectOk select
  "Activates publishing confirmations on given channel."
  ([^Channel channel]
     (SelectOk. (.confirmSelect channel)))
  ([^Channel channel ack-handler nack-handler]
     (let [select-ok (.confirmSelect channel)
           cl        (listener ack-handler nack-handler)]
       (.addConfirmListener channel cl)
       (SelectOk. select-ok))))

(defn wait-for-confirms
  "Wait until all messages published since the last call have been
   either ack'd or nack'd by the broker. Note, when called on a
   non-Confirm channel, waitForConfirms throws an IllegalStateException.

   Returns true if all messages were acked successfully,
   false otherwise."
  ([^Channel channel]
     (.waitForConfirms channel))
  ([^Channel channel ^long timeout]
     (.waitForConfirms channel timeout)))

(defn wait-for-confirms-or-die
  "Wait until all messages published since the last call have been
   either ack'd or nack'd by the broker. If any of the messages were
   nack'd, waitForConfirmsOrDie will throw an IOException. When called on
   a non-Confirm channel, it will throw an IllegalStateException."
  ([^Channel channel]
     (.waitForConfirmsOrDie channel))
  ([^Channel channel ^long timeout]
     (.waitForConfirmsOrDie channel timeout)))
