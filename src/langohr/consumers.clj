;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.consumers
  (:refer-clojure :exclude [get])
  (:import (com.rabbitmq.client Channel Consumer DefaultConsumer QueueingConsumer ShutdownSignalException Envelope AMQP$BasicProperties QueueingConsumer$Delivery))
  (:use [langohr.basic :as lhb]))




;;
;; API
;;

(defn create-default
  "Instantiates and returns a new consumer that handles various consumer life cycle events. See also langohr.basic/consume."
  ^Consumer [^Channel channel &{ :keys [consume-ok-fn cancel-fn cancel-ok-fn shutdown-signal-fn recover-ok-fn handle-delivery-fn] }]
  (proxy [DefaultConsumer] [^Channel channel]
    (handleConsumeOk [^String consumer-tag]
      (when consume-ok-fn
        (consume-ok-fn consumer-tag)))

    (handleCancelOk [^String consumer-tag]
      (when cancel-ok-fn
        (cancel-ok-fn consumer-tag)))


    (handleCancel [^String consumer-tag]
      (when cancel-fn
        (cancel-fn consumer-tag)))

    (handleRecoverOk []
      (when recover-ok-fn
        (recover-ok-fn)))

    (handleShutdownSignal [^String consumer-tag, ^ShutdownSignalException sig]
      (when shutdown-signal-fn
        (shutdown-signal-fn consumer-tag sig)))

    (handleDelivery [^String consumer-tag, ^Envelope envelope, ^AMQP$BasicProperties properties, ^bytes body]
      (when handle-delivery-fn
        (let [delivery (QueueingConsumer$Delivery. envelope properties body)]
          (handle-delivery-fn delivery properties body))))))


(defn subscribe
  "Adds new blocking consumer to a queue using basic.consume AMQP method"
  [^Channel channel, ^String queue, ^clojure.lang.IFn message-handler, & { :keys [consumer-tag, auto-ack, exclusive, no-local, arguments]
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
