;; This source code is dual-licensed under the Apache License, version
;; 2.0, and the Eclipse Public License, version 1.0.
;;
;; The APL v2.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2024 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
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
;; Copyright (c) 2011-2024 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;; All rights reserved.
;;
;; This program and the accompanying materials are made available under the terms of
;; the Eclipse Public License Version 1.0,
;; which accompanies this distribution and is available at
;; http://www.eclipse.org/legal/epl-v10.html.
;; ----------------------------------------------------------------------------------

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


  com.rabbitmq.client.Delivery
  (to-message-metadata [input]
    (merge (to-message-metadata (.getProperties input))
           (to-message-metadata (.getEnvelope input))))

  com.rabbitmq.client.GetResponse
  (to-message-metadata [input]
    (merge (to-message-metadata (.getProps input))
           (to-message-metadata (.getEnvelope input))
           {:message-count (.getMessageCount input)})))
