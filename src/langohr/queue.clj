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
