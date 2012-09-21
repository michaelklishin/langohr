(ns langohr.test.consumers-test
  (:import [com.rabbitmq.client Connection Consumer AMQP$Queue$DeclareOk])
  (:require [langohr.queue     :as lhq]
            [langohr.core      :as lhc]
            [langohr.channel   :as lch]
            [langohr.basic     :as lhb]
            [langohr.util      :as lhu]
            [langohr.consumers :as lhcons])
  (:use clojure.test))


;;
;; API
;;

(defonce conn (lhc/connect))


(deftest t-consume-ok-handler
  (let [channel  (lch/open conn)
        queue    (.getQueue (lhq/declare channel))
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :consume-ok-fn (fn [consumer-tag]
                                                                 (.countDown latch)))]
    (lhb/consume channel queue consumer)
    (.await latch)))


(deftest t-cancel-ok-handler
  (let [channel  (lch/open conn)
        queue    (.getQueue (lhq/declare channel))
        tag      (lhu/generate-consumer-tag "t-cancel-ok-handler")
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :cancel-ok-fn (fn [consumer-tag]
                                                                (.countDown latch)))
        ret-tag  (lhb/consume channel queue consumer :consumer-tag tag)]
    (is (= tag ret-tag))
    (lhb/cancel channel tag)
    (.await latch)))


(deftest t-cancel-notification-handler
  (let [channel  (lch/open conn)
        queue    (.getQueue (lhq/declare channel))
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :cancel-fn (fn [consumer_tag]
                                                             (.countDown latch)))]
    (lhb/consume channel queue consumer)
    (lhq/delete channel queue)
    (.await latch)))


(deftest t-delivery-handler
  (let [channel    (lch/open conn)
        exchange   ""
        payload    ""
        queue      (.getQueue (lhq/declare channel "" :auto-delete true))
        n          300
        latch      (java.util.concurrent.CountDownLatch. (inc n))
        consumer   (lhcons/create-default channel
                                          :handle-delivery-fn (fn [delivery message-properties message-payload]
                                                                (.countDown latch))
                                          :consume-ok-fn      (fn [consumer-tag]
                                                                (.countDown latch)))]
    (lhb/consume channel queue consumer)
    (.start (Thread. (fn []
                       (dotimes [i n]
                         (lhb/publish channel exchange queue payload))) "publisher"))
    (.await latch)))
