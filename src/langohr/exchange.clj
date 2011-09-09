(ns langohr.exchange
  (:refer-clojure :exclude [declare])
  (:import (com.rabbitmq.client Channel AMQP$Exchange$DeclareOk AMQP$Exchange$DeleteOk)))

;;
;; API
;;

(defn ^AMQP$Exchange$DeclareOk declare
  "Declares an exchange using exchange.declare AMQP method"
  ([^Channel channel ^String name ^String type]
     (.exchangeDeclare channel name type))
  ([^Channel channel ^String name ^String type { :keys [durable auto-delete internal arguments] :or {durable false, auto-delete false, internal false} }]
     (.exchangeDeclare channel name type durable auto-delete internal arguments)))


(defn ^AMQP$Exchange$DeleteOk delete
  "Deletes an exchange using exchange.delete AMQP method"
  ([^Channel channel ^String name]
     (.exchangeDelete channel name))
  ([^Channel channel ^String name if-unused]
     (.exchangeDelete channel name if-unused)))
