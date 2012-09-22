;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.consumers
  (:import [com.rabbitmq.client Channel Consumer DefaultConsumer QueueingConsumer QueueingConsumer$Delivery ShutdownSignalException Envelope AMQP$BasicProperties QueueingConsumer$Delivery])
  (:require [langohr.basic :as lhb])
  (:use langohr.conversion))




;;
;; API
;;

(defn ^Consumer create-default
  "Instantiates and returns a new consumer that handles various consumer life cycle events. See also langohr.basic/consume."
  [^Channel channel &{ :keys [consume-ok-fn cancel-fn cancel-ok-fn shutdown-signal-fn recover-ok-fn handle-delivery-fn] }]
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

    (handleShutdownSignal [^String consumer-tag ^ShutdownSignalException sig]
      (when shutdown-signal-fn
        (shutdown-signal-fn consumer-tag sig)))

    (handleDelivery [^String consumer-tag ^Envelope envelope ^AMQP$BasicProperties properties ^bytes body]
      (when handle-delivery-fn
        (let [delivery (QueueingConsumer$Delivery. envelope properties body)]
          (handle-delivery-fn channel (to-message-metadata delivery) body))))))

(defn subscribe
  "Adds new blocking default consumer to a queue using basic.consume AMQP method"
  [^Channel channel ^String queue f & {:as options}]
  (let [keys      [:handle-consume-ok :handle-cancel :handle-cancel-ok :handle-recover-ok :handle-shutdown-signal]
        cons-opts (select-keys options keys)
        options'  (apply dissoc (concat [options] keys))
        consumer  (create-default channel
                                 :handle-delivery-fn f
                                 :handle-consume-ok      (get cons-opts :handle-consume-ok)
                                 :handle-cancel-ok       (get cons-opts :handle-cancel-ok)
                                 :handle-cancel          (get cons-opts :handle-cancel)
                                 :handle-recover-ok      (get cons-opts :handle-recover-ok)
                                 :handle-shutdown-signal (get cons-opts :handle-shutdown-signal))]
    (apply lhb/consume channel queue consumer (flatten (vec options')))))
