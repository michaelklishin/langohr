;; Copyright (c) 2011-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

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
  (to-message-metadata [input]
    {:delivery-tag (.getDeliveryTag input)
     :redelivery?  (.isRedeliver input)
     :exchange     (.getExchange input)
     :routing-key  (.getRoutingKey input)})


  com.rabbitmq.client.AMQP$BasicProperties
  (to-message-metadata [input]
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
  (to-message-metadata [input]
    (merge (to-message-metadata (.getProperties input))
           (to-message-metadata (.getEnvelope input))))

  com.rabbitmq.client.GetResponse
  (to-message-metadata [input]
    (merge (to-message-metadata (.getProps input))
           (to-message-metadata (.getEnvelope input))
           {:message-count (.getMessageCount input)})))
