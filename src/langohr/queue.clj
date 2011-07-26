(ns langohr.queue
  (:import (com.rabbitmq.client Channel)))

;;
;; API
;;

(defn declare
  "Declares a queue using queue.declare AMQP method"
  ([^Channel channel]
     (.queueDeclare channel))
  ([^Channel channel ^String queue {:keys [durable auto-delete exclusive arguments] :or {durable false, auto-delete true, exclusive true}}]
     (.queueDeclare channel queue durable exclusive auto-delete arguments)))


(defn bind
  "Binds a queue to an exchange using queue.bind AMQP method"
  ([^Channel channel ^String queue ^String exchange]
     (.queueBind channel queue exchange ""))
  ([^Channel channel ^String queue ^String exchange { :keys [routing-key arguments] :or { routing-key "", arguments nil } }]
     (.queueBind channel queue exchange routing-key arguments)))



