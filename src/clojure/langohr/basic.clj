;; Copyright (c) 2011-2013 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.basic
  (:refer-clojure :exclude [get])
  (:require langohr.util
            [langohr.conversion :refer [to-message-metadata]]
            [clojurewerkz.support.bytes :refer [to-byte-array]])
  (:import [com.rabbitmq.client AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder Consumer GetResponse ReturnListener]
           [java.util Map Date]
           com.novemberain.langohr.Channel))


;;
;; API
;;

(defn publish
  "Publishes a message using basic.publish AMQP 0.9.1 method.

  This method publishes a message to a specific exchange. The message will be routed to queues as defined by
  the exchange configuration and distributed to any active consumers when the transaction, if any, is committed.

  ^String :exchange: name of the exchange to publish to. Can be an empty string, which means default exchange.
  ^String :routing-key: the routing key for the message. Used for ourting messages depending on exchange configuration.

  Payload can be anything the langohr.conversion/BytePayload protocol is extended for, Langohr ships with
  an implementation for byte arrays and strings.

  Options:
  ^Boolean :mandatory (default false): specifies reaction of server if the message can't be routed to a queue.
  ^Boolean :immediate (default false): specifies reaction of server if the message can't be delivered to a consumer immediately.


  Basic properties:

    ^String :content content-type: MIME Content type
    ^String :content-encoding: MIME Content encoding
    ^Map :headers: headers that will be passed to subscribers, given in Map format.
    ^Integer :persistent: should this message be persisted to disk?
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

      (lhb/publish channel exchange queue payload :priority 8 :message-id msg-id :content-type content-type :headers { \"see you soon\" \"à bientôt\" })"
  [^Channel channel ^String exchange ^String routing-key payload
   &{:keys [^Boolean mandatory ^Boolean immediate ^String content-type ^String ^String content-encoding ^Map headers
            ^Boolean persistent ^Integer priority ^String correlation-id ^String reply-to ^String expiration ^String message-id
            ^Date timestamp ^String type ^String user-id ^String app-id ^String cluster-id]
     :or {mandatory false immediate false}}]
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
                   immediate (.build pb) bytes)))


(defn ^ReturnListener return-listener
  "Creates new return listener. Usually used in order to be notified of failed deliveries when basic-publish is called with :mandatory or :immediate flags set, but
   message coudn't be delivered.

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

     ^String :consumer-tag: a unique consumer (subscription) identifier. Pass an empty string to make RabbitMQ generate one for you.
     ^Boolean :auto-ack (default false): true if the server should consider messages acknowledged once delivered, false if server should expect explicit acknowledgements.
     ^Boolean :exclusive (default false): true if this is an exclusive consumer (no other consumer can consume given queue)"
  [^Channel channel ^String queue ^Consumer consumer &{:keys [consumer-tag auto-ack exclusive arguments no-local]
                                                       :or {consumer-tag "" auto-ack false exclusive false no-local false}}]
  (.basicConsume ^Channel channel ^String queue ^Boolean auto-ack ^String consumer-tag ^Boolean no-local ^Boolean exclusive ^Map arguments ^Consumer consumer))


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

(defn recover-async
  [^Channel channel ^Boolean requeue]
  (.basicRecoverAsync channel requeue))
