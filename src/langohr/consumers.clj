(ns langohr.consumers
  (:import (com.rabbitmq.client Channel QueueingConsumer))
  (:use [langohr.basic :as lhb]))


;;
;; API
;;

(defn subscribe
  "Adds new blocking consumer to a queue using basic.consume AMQP method"
  [^Channel channel, ^String queue, ^clojure.lang.IFn message-handler, { :keys [consumer-tag, auto-ack, exclusive, no-local, arguments]
                                                                         :or { consumer-tag "", auto-ack false, exclusive false, no-local false } }]
  (let [queueing-consumer (QueueingConsumer. channel)]
    (do
      (lhb/consume channel queue queueing-consumer :consumer-tag consumer-tag :auto-ack auto-ack :exclusive exclusive, :arguments arguments, :no-local no-local)
      (while true
        (try
          (let [delivery (.nextDelivery queueing-consumer)]
            (message-handler delivery (.getProperties delivery) (.getBody delivery)))
          (catch InterruptedException e
            nil))))))
