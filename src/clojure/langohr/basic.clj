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

(ns langohr.basic
  "Functions that cover basic.* protocol methods: publishing and consumption
   of messages, acknowledgements.

   Relevant guides:

   * http://clojurerabbitmq.info/articles/queues.html
   * http://clojurerabbitmq.info/articles/exchanges.html"
  (:refer-clojure :exclude [get])
  (:require [langohr.conversion :refer [to-message-metadata]]
            [clojurewerkz.support.bytes :refer [to-byte-array]])
  (:import [com.rabbitmq.client AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder Consumer GetResponse ReturnListener]
           [java.util Map Date]
           com.rabbitmq.client.Channel))


;;
;; API
;;

(defn publish
  "Publishes a message using basic.publish AMQP 0.9.1 method.

  This method publishes a message to a specific exchange. The message will be routed to queues as defined by
  the exchange configuration and distributed to any active consumers when the transaction, if any, is committed.

  ^String :exchange: name of the exchange to publish to. Can be an empty string, which means default exchange.
  ^String :routing-key: the routing key for the message. Used for routing messages depending on exchange configuration.

  Payload can be anything the clojurewerkz.support.bytes/ByteSource protocol is extended for, Langohr ships with
  an implementation for byte arrays and strings.

  Options:
  ^Boolean :mandatory (default false): specifies reaction of server if the message can't be routed to a queue.


  Basic properties:

    ^String :content content-type: MIME Content type
    ^String :content-encoding: MIME Content encoding
    ^Map :headers: headers that will be passed to subscribers, given in Map format.
    ^Boolean :persistent: should this message be persisted to disk?
    ^Integer :priority: message priority, number from 0 to 9
    ^String :correlation-id: application correlation identifier. Useful for cases when it's required to match request with the response.
                             Usually a unique value.
    ^String :reply-to: name of the reply queue.
    ^String :expiration: how long a message is valid
    ^String :message-id: message identifier
    ^Date :timestamp: timestamp associated with this message
    ^String :type: message type, can be in any format, e.g. search.requests.index
    ^String :user-id: user ID. RabbitMQ will validate this against the active connection user
    ^String :app-id: publishing application ID

  Example:

      (lhb/publish channel exchange queue payload {:priority 8 :message-id msg-id :content-type content-type :headers { \"key\" \"value\" }})"
  ([^Channel ch ^String exchange ^String routing-key payload]
     (publish ch exchange routing-key payload {}))
  ([^Channel channel ^String exchange ^String routing-key payload
    {:keys [^Boolean mandatory ^String content-type ^String ^String content-encoding ^Map headers
            ^Boolean persistent ^Integer priority ^String correlation-id ^String reply-to ^String expiration ^String message-id
            ^Date timestamp ^String type ^String user-id ^String app-id ^String cluster-id]
     :or {mandatory false}}]
     (let [bytes (to-byte-array payload)
           pb    (doto (AMQP$BasicProperties$Builder.)
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
                   (.clusterId       cluster-id))]
       (.basicPublish channel
                      exchange
                      routing-key
                      mandatory
                      (.build pb) bytes))))


(defn ^ReturnListener return-listener
  "Creates new return listener. Usually used in order to be notified of failed deliveries when basic-publish is called with :mandatory or :immediate flags set, but
   message couldn't be delivered.

   If the client has not configured a return listener for a particular channel, then the associated returned message will be silently dropped.

   f: a handler function that accepts reply-code, reply-text, exchange, routing-key, properties and body as arguments.


   Example:

      (let [f (langohr.basic/return-listener (fn [reply-code reply-text exchange routing-key properties body]
                                               (println reply-code reply-text exchange routing-key properties body)))]
        (.addReturnListener ch f))"
  [^clojure.lang.IFn f]
  (reify ReturnListener
    (handleReturn [this reply-code reply-text exchange routing-key properties body]
      (f reply-code reply-text exchange routing-key properties body))))

(defn ^Channel add-return-listener
  "Adds return listener to the given channel"
  [^Channel channel ^clojure.lang.IFn f]
  (.addReturnListener channel (return-listener f))
  channel)



(defn ^String consume
  "Adds new consumer to a queue using basic.consume AMQP 0.9.1 method.

   Called with default parameters, starts non-nolocal, non-exclusive consumer with explicit acknowledgement and server-generated consumer tag.

   ^String queue - the name of the queue
   ^Consumer consumer - callback to receive notifications and messages from a queue by subscription. For more information about consumers, check out langohr.consumers ns.

   Options:

     ^String :consumer-tag: a unique consumer (subscription) identifier.
                            Omit the option or pass an empty string to make RabbitMQ generate one for you.
     ^Boolean :auto-ack (default false): true if the server should consider messages acknowledged once delivered,
                                         false if server should expect manual acknowledgements.
     ^Boolean :exclusive (default false): true if this is an exclusive consumer
                                          (no other consumer can consume given queue)"
  ([^Channel ch ^String queue ^Consumer consumer]
     (consume ch queue consumer {}))
  ([^Channel ch ^String queue ^Consumer consumer {:keys [consumer-tag auto-ack exclusive arguments no-local]
                                                  :or {consumer-tag "" auto-ack false exclusive false no-local false}}]
     (.basicConsume ^Channel ch
                    ^String queue
                    ^Boolean auto-ack
                    ^String consumer-tag
                    ^Boolean no-local
                    ^Boolean exclusive
                    ^Map arguments
                    ^Consumer consumer)))


(defn cancel
  "Cancels a consumer (subscription) using basic.cancel AMQP 0.9.1 method"
  [^Channel channel ^String consumer-tag]
  (.basicCancel ^Channel channel ^String consumer-tag))


(defn get
  "Fetches a message from a queue using basic.get AMQP 0.9.1 method"
  ([^Channel channel ^String queue]
     (when-let [response (.basicGet channel queue true)]
       [(to-message-metadata response) (.getBody response)]))
  ([^Channel channel ^String queue ^Boolean auto-ack]
     (when-let [response (.basicGet channel queue auto-ack)]
       [(to-message-metadata response) (.getBody response)])))



(defn qos
  "Sets channel or connection prefetch level using basic.qos AMQP 0.9.1 method"
  ([^Channel channel ^long prefetch-count]
     (.basicQos channel prefetch-count))
  ([^Channel channel ^long prefetch-size ^long prefetch-count ^Boolean global]
     (.basicQos channel prefetch-size prefetch-count global)))


(defn ack
  "Acknowledges one or more messages using basic.ack AMQP 0.9.1 method"
  ([^Channel channel ^long delivery-tag]
     (.basicAck channel delivery-tag false))
  ([^Channel channel ^long delivery-tag multiple]
     (.basicAck channel delivery-tag multiple)))

(defn reject
  "Rejects (and, optionally, requeues) a messages using basic.reject AMQP 0.9.1 method"
  ([^Channel channel ^long delivery-tag]
     (.basicReject channel delivery-tag false))
  ([^Channel channel ^long delivery-tag ^Boolean requeue]
     (.basicReject channel delivery-tag requeue)))


(defn nack
  "Negative acknowledgement of one or more messages using basic.nack AMQP 0.9.1 methods (a RabbitMQ-specific extension)"
  [^Channel channel ^long delivery-tag multiple ^Boolean requeue]
  (.basicNack channel delivery-tag multiple requeue))


(defn recover
  "Notifies RabbitMQ that it needs to redeliver unacknowledged messages using basic.recover AMQP 0.9.1 method"
  ([^Channel channel]
     (.basicRecover channel))
  ([^Channel channel ^Boolean requeue]
     (.basicRecover channel requeue)))
