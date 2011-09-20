;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.basic
  (:refer-clojure :exclude [get])
  (:require [langohr util])
  (:import (com.rabbitmq.client Channel AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder Consumer GetResponse AMQP$Basic$RecoverOk)
           (java.util Map)))


;;
;; API
;;

(defn publish
  "Publishes a message using basic.publish AMQP method"
  [^Channel channel, ^String exchange, ^String routing-key, ^String payload,
   &{:keys [content-type content-encoding headers
            persistent priority correlation-id reply-to expiration message-id
            timestamp type user-id app-id cluster-id]}]
  (let [payload-bytes      (.getBytes payload)
        properties-builder (AMQP$BasicProperties$Builder.)
        properties         (.build (doto properties-builder
                                     (.contentType     content-type)
                                     (.contentEncoding content-encoding)
                                     (.headers         headers)
                                     (.deliveryMode    (Integer/valueOf (if persistent 2 1)))
                                     (.priority        (if priority (Integer/valueOf ^Long priority) nil))
                                     (.correlationId   correlation-id)
                                     (.replyTo         reply-to)
                                     (.expiration      expiration)
                                     (.messageId       message-id)
                                     (.timestamp       timestamp)
                                     (.type            type)
                                     (.userId          user-id)
                                     (.appId           app-id)
                                     (.clusterId       cluster-id)))]
    (.basicPublish channel exchange routing-key properties payload-bytes)))


(defn consume
  "Adds new consumer to a queue using basic.consume AMQP method"
  (^String [^Channel channel, ^String queue, ^Consumer consumer, &{ :keys [consumer-tag auto-ack exclusive arguments no-local]
                                                                   :or { consumer-tag "", auto-ack false, exclusive false, no-local false } }]
           (.basicConsume ^Channel channel ^String queue ^Boolean auto-ack ^String consumer-tag ^Boolean no-local ^Boolean exclusive ^Map arguments ^Consumer consumer)))


(defn cancel
  "Cancels consumer using basic.cancel AMQP method"
  [^Channel channel, ^String consumer-tag]
  (.basicCancel ^Channel channel ^String consumer-tag))


(defn get
  "Fetches a message from a queue using basic.get AMQP method"
  (^GetResponse [^Channel channel, ^String queue]
                (.basicGet channel queue true))
  (^GetResponse [^Channel channel, ^String queue, auto-ack]
                (.basicGet channel queue auto-ack)))



(defn qos
  "Sets channel or connection prefetch level using basic.qos AMQP method"
  ([^Channel channel ^long prefetch-count]
     (.basicQos channel prefetch-count))
  ([^Channel channel ^long prefetch-size ^long prefetch-count global]
     (.basicQos channel prefetch-size prefetch-count global)))


(defn ack
  "Acknowledges one or more messages using basic.ack AMQP method"
  ([^Channel channel ^long delivery-tag]
     (.basicAck channel delivery-tag false))
  ([^Channel channel ^long delivery-tag multiple]
     (.basicAck channel delivery-tag multiple)))

(defn reject
  "Rejects (and, optionally, requeues) a messages using basic.reject AMQP method"
  ([^Channel channel ^long delivery-tag]
     (.basicAck channel delivery-tag false))
  ([^Channel channel ^long delivery-tag requeue]
     (.basicAck channel delivery-tag requeue)))


(defn nack
  "Negative acknowledgement of one or more messages using basic.nack AMQP methods (a RabbitMQ extension to AMQP 0.9.1"
  [^Channel channel ^long delivery-tag multiple requeue]
  (.basicNack channel delivery-tag multiple requeue))


(defn recover
  (^AMQP$Basic$RecoverOk [^Channel channel]
                         (.basicRecover channel))
  (^AMQP$Basic$RecoverOk [^Channel channel, ^Boolean requeue]
                         (.basicRecover channel requeue)))

(defn recover-async
  [^Channel channel, ^Boolean requeue]
  (.basicRecoverAsync channel requeue))
