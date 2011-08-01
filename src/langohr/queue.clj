(ns langohr.queue
  (:import (com.rabbitmq.client Channel AMQP$Queue$DeclareOk AMQP$Queue$BindOk AMQP$Queue$DeleteOk)))

;;
;; API
;;

(defn ^AMQP$Queue$DeclareOk declare
  "Declares a queue using queue.declare AMQP method"
  ([^Channel channel]
     (.queueDeclare channel))
  ([^Channel channel ^String queue]
     (.queueDeclare channel queue false true true nil))
  ([^Channel channel ^String queue {:keys [durable exclusive auto-delete arguments] :or {durable false, exclusive true, auto-delete true}}]
     (.queueDeclare channel queue durable exclusive auto-delete arguments)))


(defn ^AMQP$Queue$BindOk bind
  "Binds a queue to an exchange using queue.bind AMQP method"
  ([^Channel channel ^String queue ^String exchange]
     (.queueBind channel queue exchange ""))
  ([^Channel channel ^String queue ^String exchange { :keys [routing-key arguments] :or { routing-key "", arguments nil } }]
     (.queueBind channel queue exchange routing-key arguments)))


(defn ^AMQP$Queue$DeleteOk delete
  "Deletes a queue using queue.delete AMQP method"
  ([^Channel channel ^String queue]
     (.queueDelete channel queue))
  ([^Channel channel ^String queue if-unused if-empty]
     (.queueDelete channel queue if-unused if-empty)))


