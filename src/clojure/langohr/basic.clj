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
  (:require langohr.util)
  (:use [langohr.conversion :only [to-bytes to-message-metadata]])
  (:import [com.rabbitmq.client Channel AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder Consumer GetResponse ReturnListener]
           [java.util Map Date]))


;;
;; API
;;

(defn publish
  "Publishes a message using basic.publish AMQP method.

  This method publishes a message to a specific exchange. The message will be routed to queues as defined by the exchange configuration and distributed to any active consumers when the transaction, if any, is committed.

  ^String :exchange - name of the exchange to publish to. Can be empty, which means default exchange.
  ^String :routing-key - the routing key for the message. Used for ourting messages depending on exchange configuration.

  Payload can be anything the langohr.conversion/BytePayload protocol is extended for, Langohr ships with
  an implementation for byte arrays and strings.

  Options:
  ^Boolean :mandatory, default false - specifies reaction of server if the message can't be routed to a queue. True when requesting a mandatory publish
  ^Boolean :immediate, default false - specifies reaction of server if the message can't be routed to a queue consumer immediately. True when requesting an immediate publish.


  Basic properties:

    ^String :content content-type - MIME Content type
    ^String :content-encoding - MIME Content encoding
    ^Map :headers - headers that will be passed to subscribers, given in Map format.
    ^Integer :persistent - indicates delivery mode for the message. Non-persistent 1, persistent - 2.
    ^Integer :priority - message priority, number from 0 to 9
    ^String :correlation-id - application correlation identifier. Useful for cases when it's required to match request with the response. Usually it's a unique value.
    ^String :reply-to - name of the callback queue. RabbitMQ spec says: address to reply to.
    ^String :expiration - Docs TBD
    ^String :message-id - application Message identifier
    ^Date :timestamp - message timestamp
    ^String :type - message type name
    ^String :user-id - creating user ID
    ^String :app-id - creating application ID
    ^String :cluster-id - Docs TBD

  EXAMPLE:

      (lhb/publish channel exchange queue payload :priority 8, :message-id msg-id, :content-type content-type, :headers { \"see you soon\" \"à bientôt\" })

  "
  [^Channel channel ^String exchange ^String routing-key payload
   &{:keys [^Boolean mandatory ^Boolean immediate ^String content-type ^String ^String content-encoding ^Map headers
            ^Boolean persistent ^Integer priority ^String correlation-id ^String reply-to ^String expiration ^String message-id
            ^Date timestamp ^String type ^String user-id ^String app-id ^String cluster-id]
     :or {mandatory false immediate false}}]
  (let [payload-bytes      (to-bytes payload)
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
    (.basicPublish channel exchange routing-key mandatory immediate  properties payload-bytes)))


(defn return-listener
  "Creates new return listener. Usually used in order to be notified of failed deliveries when basic-publish is called with :mandatory or :immediate flags set, but
   message coudn't be delivered.

   If the client has not configured a return listener for a particular channel, then the associated returned message will be silently dropped.

   ^IFn :handler-fn - callback/handler funciton, with :reply-code, :reply-text, :exchange, :routing-key, :properties and :body set.


   EXAMPLE:
      (let [listener (langohr.basic/return-listener (fn [reply-code, reply-text, exchange, routing-key, properties, body]
                                                      (println reply-code, reply-text, exchange, routing-key, properties, body))) ]
        (.addReturnListener channel listener))

  "
  [^clojure.lang.IFn f]
  (reify ReturnListener
    (handleReturn [this reply-code reply-text exchange routing-key properties body]
      (f reply-code reply-text exchange routing-key properties body))))

(defn add-return-listener
  "Adds return listener to the given channel"
  [^Channel channel ^clojure.lang.IFn f]
  (.addReturnListener channel (return-listener f))
  channel)



(defn ^String consume
  "Adds new consumer to a queue using basic.consume AMQP method.

   Called with default parameters, starts non-nolocal, non-exclusive consumer with explicit acknowledgement and server-generated consumer tag.

   ^String queue - the name of the queue
   ^Consumer consumer - callback to receive notifications and messages from a queue by subscription. For more information about consumers, check out langohr.consumers ns.

   Options:

     ^String :consumer-tag
     ^Boolean :auto-ack (default false) - true if the server should consider messages acknowledged once delivered, false if server should expect explicit acknowledgements.
     ^Boolean :exclusive (default false) - true if this is an exclusive consumer (no other consumer can consume given queue)
     ^Boolean :no-local (default false) - flag set to true unless server local buffering is required.

"
  [^Channel channel ^String queue ^Consumer consumer &{:keys [consumer-tag auto-ack exclusive arguments no-local]
                                                       :or {consumer-tag "" auto-ack false exclusive false no-local false}}]
  (.basicConsume ^Channel channel ^String queue ^Boolean auto-ack ^String consumer-tag ^Boolean no-local ^Boolean exclusive ^Map arguments ^Consumer consumer))


(defn cancel
  "Cancels consumer using basic.cancel AMQP method"
  [^Channel channel ^String consumer-tag]
  (.basicCancel ^Channel channel ^String consumer-tag))


(defn get
  "Fetches a message from a queue using basic.get AMQP method"
  ([^Channel channel ^String queue]
     (when-let [response (.basicGet channel queue true)]
       [(to-message-metadata response) (.getBody response)]))
  ([^Channel channel ^String queue ^Boolean auto-ack]
     (when-let [response (.basicGet channel queue auto-ack)]
       [(to-message-metadata response) (.getBody response)])))



(defn qos
  "Sets channel or connection prefetch level using basic.qos AMQP method"
  ([^Channel channel ^long prefetch-count]
     (.basicQos channel prefetch-count))
  ([^Channel channel ^long prefetch-size ^long prefetch-count ^Boolean global]
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
     (.basicReject channel delivery-tag false))
  ([^Channel channel ^long delivery-tag ^Boolean requeue]
     (.basicReject channel delivery-tag requeue)))


(defn nack
  "Negative acknowledgement of one or more messages using basic.nack AMQP methods (a RabbitMQ extension to AMQP 0.9.1"
  [^Channel channel ^long delivery-tag multiple ^Boolean requeue]
  (.basicNack channel delivery-tag multiple requeue))


(defn recover
  "Notifies RabbitMQ that it needs to redeliver unacknowledged messages using basic.recover AMQP method"
  ([^Channel channel]
     (.basicRecover channel))
  ([^Channel channel ^Boolean requeue]
     (.basicRecover channel requeue)))

(defn recover-async
  [^Channel channel ^Boolean requeue]
  (.basicRecoverAsync channel requeue))
