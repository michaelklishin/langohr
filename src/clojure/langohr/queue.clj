;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.queue
  (:refer-clojure :exclude [declare])
  (:import [com.rabbitmq.client Channel AMQP$Queue$DeclareOk AMQP$Queue$BindOk AMQP$Queue$UnbindOk AMQP$Queue$DeleteOk AMQP$Queue$PurgeOk]
           java.util.Map))

;;
;; API
;;

(defn ^AMQP$Queue$DeclareOk declare
  "Actively declare a server-named or named queue using queue.declare AMQP method.

   Usage example:

       ;; declare server-named, exclusive, autodelete, non-durable queue.
       (lhq/declare channel) ;; will yield a name like: 'amq.gen-QtE7OdDDjlHcxNGWuSoUb3'

       ;; creates named non-durable, exclusive, autodelete queue
       (lhq/declare channel queue-name :durable false :exclusive true :auto-delete true)

   Options

    :durable (default: false): indicates wether the queue is durable. Durable queue will survive server restart. Durable queues do not neccessarily hold persistent messages. Using persistent messages with transient queues is allowed, but will not save messages between restarts.
    :exclusive (default: false): when set to true, indicates that the queue is exclusive. No other subscriber can consume form that queue. Exclusive always implies auto-delete, as messages are delivered to the single consumer. When set to false, allows multiple consumers.
    :auto-delete (default: true): when set to true, queue will be purged as soon as last consumer stops is finished using it. If consumer never got attached to the queue, it won't get deleted.

    :arguments: other properties for the Queue.
  "
  ([^Channel channel]
     (.queueDeclare channel))
  ([^Channel channel ^String queue]
     (.queueDeclare channel queue false true true nil))
  ([^Channel channel ^String queue &{:keys [^Boolean durable ^Boolean exclusive ^Boolean auto-delete arguments] :or {durable false, exclusive true, auto-delete true}}]
     (.queueDeclare channel queue durable exclusive auto-delete arguments)))


(defn declare-passive
  "Declares a queue passively (checks that it is there) using queue.declare AMQP method"
  [^Channel channel ^String queue]
  (.queueDeclarePassive channel queue))


(defn ^AMQP$Queue$BindOk bind
  "Binds a queue to an exchange using queue.bind AMQP method"
  ([^Channel channel ^String queue ^String exchange]
     (.queueBind channel queue exchange ""))
  ([^Channel channel ^String queue ^String exchange &{ :keys [routing-key arguments] :or { routing-key "", arguments nil } }]
     (.queueBind channel queue exchange routing-key arguments)))


(defn ^AMQP$Queue$UnbindOk unbind
  "Unbinds a queue from an exchange using queue.bind AMQP method"
  ([^Channel channel ^String queue ^String exchange ^String routing-key]
     (.queueUnbind channel queue exchange routing-key))
  ([^Channel channel ^String queue ^String exchange ^String routing-key ^Map arguments]
     (.queueUnbind channel queue exchange routing-key arguments)))


(defn ^AMQP$Queue$DeleteOk delete
  "Deletes a queue using queue.delete AMQP method"
  ([^Channel channel ^String queue]
     (.queueDelete channel queue))
  ([^Channel channel ^String queue if-unused if-empty]
     (.queueDelete channel queue if-unused if-empty)))


(defn ^AMQP$Queue$PurgeOk purge
  "Purges a queue using queue.purge AMQP method"
  [^Channel channel ^String queue]
  (.queuePurge channel queue))


(defn status
  "Returns a map with two keys: message-count and :consumer-count, for the given queue. Uses queue.declare AMQP method with the :passive attribute set."
  [^Channel channel ^String queue]
  (let [declare-ok ^AMQP$Queue$DeclareOk (.queueDeclarePassive channel queue)]
    { :message-count (.getMessageCount declare-ok), :consumer-count (.getConsumerCount declare-ok) }))
