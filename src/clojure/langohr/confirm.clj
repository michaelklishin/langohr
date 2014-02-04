;; Copyright (c) 2011-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other from this software.

(ns langohr.confirm
  "Functions that work with publisher confirms.

   Relevant guides:

   * http://clojurerabbitmq.info/articles/exchanges.html"
  (:import [com.rabbitmq.client ConfirmListener]
           com.novemberain.langohr.confirm.SelectOk
           com.novemberain.langohr.Channel))


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
