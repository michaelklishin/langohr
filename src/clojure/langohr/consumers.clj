;; Copyright (c) 2011-2013 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.consumers
  (:require [langohr.basic :as lhb])
  (:use langohr.conversion)
  (:import [com.rabbitmq.client Channel Consumer DefaultConsumer QueueingConsumer QueueingConsumer$Delivery ShutdownSignalException Envelope AMQP$BasicProperties QueueingConsumer$Delivery]))





;;
;; API
;;

(defn ^Consumer create-default
  "Instantiates and returns a new consumer that handles various consumer life cycle events. See also langohr.basic/consume."
  [^Channel channel &{:keys [handle-consume-ok-fn
                             handle-cancel-fn
                             handle-cancel-ok-fn
                             handle-shutdown-signal-fn
                             handle-recover-ok-fn
                             handle-delivery-fn]}]
  (proxy
      [DefaultConsumer]
      [(if (instance? com.novemberain.langohr.Channel channel)
         (.getDelegate ^com.novemberain.langohr.Channel channel)
         channel)]
    (handleConsumeOk [^String consumer-tag]
      (when handle-consume-ok-fn
        (handle-consume-ok-fn consumer-tag)))

    (handleCancelOk [^String consumer-tag]
      (when handle-cancel-ok-fn
        (handle-cancel-ok-fn consumer-tag)))


    (handleCancel [^String consumer-tag]
      (when handle-cancel-fn
        (handle-cancel-fn consumer-tag)))

    (handleRecoverOk []
      (when handle-recover-ok-fn
        (handle-recover-ok-fn)))

    (handleShutdownSignal [^String consumer-tag ^ShutdownSignalException sig]
      (when handle-shutdown-signal-fn
        (handle-shutdown-signal-fn consumer-tag sig)))

    (handleDelivery [^String consumer-tag ^Envelope envelope ^AMQP$BasicProperties properties ^bytes body]
      (when handle-delivery-fn
        (handle-delivery-fn channel (to-message-metadata (QueueingConsumer$Delivery. envelope properties body)) body)))))

(defn subscribe
  "Adds new default consumer to a queue using basic.consume AMQP 0.9.1 method"
  [^Channel channel ^String queue f & {:as options}]
  (let [keys      [:handle-consume-ok :handle-cancel :handle-cancel-ok :handle-recover-ok :handle-shutdown-signal]
        keys      (concat keys (map #(keyword (str (name %) "-fn")) keys))
        cons-opts (select-keys options keys)
        options'  (apply dissoc (concat [options] keys))
        consumer  (create-default channel
                                  :handle-delivery-fn f
                                  :handle-consume-ok-fn      (or (get cons-opts :handle-consume-ok-fn)
                                                                 (get cons-opts :handle-consume-ok))
                                  :handle-cancel-ok-fn       (or (get cons-opts :handle-cancel-ok-fn)
                                                                 (get cons-opts :handle-cancel-ok))
                                  :handle-cancel-fn          (or (get cons-opts :handle-cancel-fn)
                                                                 (get cons-opts :handle-cancel))
                                  :handle-recover-ok-fn      (or (get cons-opts :handle-recover-ok-fn)
                                                                 (get cons-opts :handle-recover-ok))
                                  :handle-shutdown-signal-fn (or (get cons-opts :handle-shutdown-signal-fn)
                                                                 (get cons-opts :handle-shutdown-signal)))]
    (apply lhb/consume channel queue consumer (flatten (vec options')))))

(defn- consumer-seq
  "Builds a lazy seq of Delivery instances from a QueueingConsumer."
  [^QueueingConsumer qcs]
  (lazy-seq (cons (.nextDelivery qcs) (consumer-seq qcs))))

(defn ack-unless-exception
  "Wrapper for delivery handlers which auto-acks messages.

   This differs from `:auto-ack true', which tells the broker to
   consider messages acked upon delivery. This explicitly acks, as
   long as the consumer function doesn't throw an exception."
  [f]
  (fn [^Channel channel {:keys [delivery-tag] :as metadata} body]
    (f channel metadata body)
    (.basicAck channel delivery-tag false)))

(defn blocking-subscribe
  "Adds new QueueingConsumer to a queue using basic.consume AMQP 0.9.1 method"
  [^Channel channel ^String queue f & options]
  (let [consumer (QueueingConsumer. channel)]
    (apply lhb/consume channel queue consumer options)
    (doseq [^QueueingConsumer$Delivery d (consumer-seq consumer)]
      (f channel (to-message-metadata d) (.getBody d)))))
