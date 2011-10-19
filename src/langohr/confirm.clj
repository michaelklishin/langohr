;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.confirm
  (:import (com.rabbitmq.client Channel AMQP$Confirm$SelectOk ConfirmListener)))


;;
;; API
;;


(defn listener
  "Instantiates and returns a new confirmations listener that handles basic.ack and basic.nack method deliveries"
  [^clojure.lang.IFn ack-handler, ^clojure.lang.IFn nack-handler]
  (reify ConfirmListener
    (handleAck [this, delivery-tag, multiple]
      (ack-handler delivery-tag multiple))
    (handleNack [this, delivery-tag, multiple]
      (nack-handler delivery-tag multiple))))


(defn add-listener
  "Adds confirmations listener to given channel"
  [^Channel channel, ^ConfirmListener cl]
  (.addConfirmListener channel cl)
  cl)


(defn select
  "Activates publishing confirmations on given channel."
  ([^Channel channel]
     (.confirmSelect channel))
  ([^Channel channel, ^clojure.lang.IFn ack-handler, ^clojure.lang.IFn nack-handler]
     (let [select-ok (.confirmSelect channel)
           cl        (listener ack-handler nack-handler)]
       (.addConfirmListener channel cl)
       select-ok)))
