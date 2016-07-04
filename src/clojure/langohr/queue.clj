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

(ns langohr.queue
  "Functions that provide operations on queues.

   Relevant guides:

   http://clojurerabbitmq.info/articles/queues.html"
  (:refer-clojure :exclude [declare empty?])
  (:import [com.rabbitmq.client AMQP$Queue$DeclareOk AMQP$Queue$BindOk AMQP$Queue$UnbindOk AMQP$Queue$DeleteOk AMQP$Queue$PurgeOk]
           java.util.Map
           [com.novemberain.langohr.queue DeclareOk BindOk UnbindOk DeleteOk PurgeOk]
           com.rabbitmq.client.Channel))

;;
;; API
;;

(defn ^com.novemberain.langohr.queue.DeclareOk declare
  "Actively declare a server-named or named queue using queue.declare AMQP method.

   Usage example:

       ;; declare server-named, exclusive, autodelete, non-durable queue.
       (lhq/declare channel) ;; will return a map that contains the name: {:queue \"amq.gen-QtE7OdDDjlHcxNGWuSoUb3\"}

       ;; creates named non-durable, exclusive, autodelete queue
       (lhq/declare channel queue-name {:durable false :exclusive true :auto-delete true})

   Options

    :durable (default: false): indicates wether the queue is durable. Durable queue will survive server restart. Durable queues do not neccessarily hold persistent messages. Using persistent messages with transient queues is allowed, but will not save messages between restarts.
    :exclusive (default: false): when set to true, indicates that the queue is exclusive. No other subscriber can consume form that queue. Exclusive always implies auto-delete, as messages are delivered to the single consumer. When set to false, allows multiple consumers.
    :auto-delete (default: true): when set to true, queue will be purged as soon as last consumer stops is finished using it. If consumer never got attached to the queue, it won't get deleted.

    :arguments: other properties for the Queue.
  "
  ([^Channel ch]
     (DeclareOk. (.queueDeclare ch)))
  ([^Channel ch ^String queue]
     (DeclareOk. (.queueDeclare ch queue false false true nil)))
  ([^Channel ch ^String queue {:keys [^Boolean durable ^Boolean exclusive ^Boolean auto-delete arguments]
                               :or {durable false exclusive false auto-delete true}}]
     (DeclareOk. (.queueDeclare ch queue durable exclusive auto-delete arguments))))


(defn ^com.novemberain.langohr.queue.DeclareOk declare-passive
  "Declares a queue passively (checks that it is there) using queue.declare AMQP method"
  [^Channel ch ^String queue]
  (DeclareOk. (.queueDeclarePassive ch queue)))

(defn ^String declare-server-named
  "Declares a server-named queue and returns its name."
  ([^Channel ch]
     (-> ch .queueDeclare .getQueue))
  ([^Channel ch {:keys [^Boolean durable ^Boolean exclusive ^Boolean auto-delete arguments]
                 :or {durable false exclusive false auto-delete true}}]
     (-> ch
         (.queueDeclare "" durable exclusive auto-delete arguments)
         .getQueue)))

(defn ^com.novemberain.langohr.queue.BindOk bind
  "Binds a queue to an exchange using queue.bind AMQP method"
  ([^Channel ch ^String queue ^String exchange]
     (bind ch queue exchange {}))
  ([^Channel ch ^String queue ^String exchange {:keys [routing-key arguments]
                                                :or {routing-key "" arguments nil}}]
     (BindOk. (.queueBind ch queue exchange routing-key arguments))))


(defn ^com.novemberain.langohr.queue.UnbindOk unbind
  "Unbinds a queue from an exchange using queue.bind AMQP method"
  ([^Channel ch ^String queue ^String exchange]
     (UnbindOk. (.queueUnbind ch queue exchange "")))
  ([^Channel ch ^String queue ^String exchange ^String routing-key]
     (UnbindOk. (.queueUnbind ch queue exchange routing-key)))
  ([^Channel ch ^String queue ^String exchange ^String routing-key ^Map arguments]
     (UnbindOk. (.queueUnbind ch queue exchange routing-key arguments))))


(defn ^com.novemberain.langohr.queue.DeleteOk delete
  "Deletes a queue using queue.delete AMQP method"
  ([^Channel ch ^String queue]
     (DeleteOk. (.queueDelete ch queue)))
  ([^Channel ch ^String queue if-unused if-empty]
     (DeleteOk. (.queueDelete ch queue if-unused if-empty))))


(defn ^com.novemberain.langohr.queue.PurgeOk purge
  "Purges a queue using queue.purge AMQP method"
  [^Channel ch ^String queue]
  (PurgeOk. (.queuePurge ch queue)))


(defn status
  "Returns a map with two keys: message-count and :consumer-count, for the given queue.
   Uses queue.declare AMQP method with the :passive attribute set."
  [^Channel ch ^String queue]
  (let [declare-ok ^AMQP$Queue$DeclareOk (.queueDeclarePassive ch queue)]
    {:message-count (.getMessageCount declare-ok) :consumer-count (.getConsumerCount declare-ok)}))

(defn message-count
  "Returns a number of messages that are ready for delivery (e.g. not pending acknowledgements)
   in the queue"
  [^Channel ch ^String queue]
  (:message-count (status ch queue)))

(defn consumer-count
  "Returns a number of active consumers on the queue"
  [^Channel ch ^String queue]
  (:consumer-count (status ch queue)))

(defn ^boolean empty?
  "Returns true if queue is empty (has no messages ready), false otherwise"
  [^Channel ch ^String queue]
  (zero? (message-count ch queue)))
