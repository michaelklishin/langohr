(ns langohr.conversion
  (:require [clojurewerkz.support.internal :as i]))


;;
;; API
;;

(def ^{:const true}
  persistent-mode 2)

(defprotocol MessageMetadata
  (to-message-metadata [input] "Turns AMQP 0.9.1 message metadata into a Clojure map"))

(extend-protocol MessageMetadata
  com.rabbitmq.client.Envelope
  (to-message-metadata [^Envelope input]
    {:delivery-tag (.getDeliveryTag input)
     :redelivery?  (.isRedeliver input)
     :exchange     (.getExchange input)
     :routing-key  (.getRoutingKey input)})


  com.rabbitmq.client.AMQP$BasicProperties
  (to-message-metadata [^AMQP$BasicProperties input]
    {:content-type     (.getContentType input)
     :content-encoding (.getContentEncoding input)
     :headers          (.getHeaders input)
     :delivery-mode    (.getDeliveryMode input)
     :persistent?      (= persistent-mode (.getDeliveryMode input))
     :priority         (.getPriority input)
     :correlation-id   (.getCorrelationId input)
     :reply-to         (.getReplyTo input)
     :expiration       (.getExpiration input)
     :message-id       (.getMessageId input)
     :timestamp        (.getTimestamp input)
     :type             (.getType input)
     :user-id          (.getUserId input)
     :app-id           (.getAppId input)
     :cluster-id       (.getClusterId input)})


  com.rabbitmq.client.QueueingConsumer$Delivery
  (to-message-metadata [^QueueingConsumer$Delivery input]
    (merge (to-message-metadata (.getProperties input))
           (to-message-metadata (.getEnvelope input)))))



(defprotocol BytePayload
  (to-bytes [input] "Converts the input to a byte array that can be sent as an AMQP 0.9.1 message payload"))

(extend-protocol BytePayload
  String
  (to-bytes [^String input]
    (.getBytes input "UTF-8")))

(extend i/byte-array-type
  BytePayload
  {:to-bytes identity})
