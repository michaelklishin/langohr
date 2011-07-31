(ns langohr.exchange
  (:import (com.rabbitmq.client Channel AMQP$Exchange$DeclareOk)))

;;
;; API
;;

(defn ^AMQP$Exchange$DeclareOk declare
  "Declares an exchange using exchange.declare AMQP method"
  ([^Channel channel ^String name ^String type]
     (.exchangeDeclare channel name type))
  ([^Channel channel ^String name ^String type { :keys [durable auto-delete internal arguments] :or {durable false, auto-delete false, internal false} }]
     (.exchangeDeclare channel name type durable auto-delete internal arguments)))
