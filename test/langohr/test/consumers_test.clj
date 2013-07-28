(ns langohr.test.consumers-test
  (:import [com.rabbitmq.client Consumer AMQP$Queue$DeclareOk])
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
        consumer (lhcons/create-default channel :handle-consume-ok-fn (fn [consumer-tag]
                                                                        (.countDown latch)))]
    (lhb/consume channel queue consumer)
    (.await latch)))


(deftest t-cancel-ok-handler
  (let [channel  (lch/open conn)
        queue    (.getQueue (lhq/declare channel))
        tag      (lhu/generate-consumer-tag "t-cancel-ok-handler")
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :handle-cancel-ok-fn (fn [consumer-tag]
                                                                       (.countDown latch)))
        ret-tag  (lhb/consume channel queue consumer :consumer-tag tag)]
    (is (= tag ret-tag))
    (lhb/cancel channel tag)
    (.await latch)))


(deftest t-cancel-notification-handler
  (let [channel  (lch/open conn)
        queue    (.getQueue (lhq/declare channel))
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :handle-cancel-fn (fn [consumer_tag]
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
                                          :handle-delivery-fn   (fn [ch metadata ^bytes payload]
                                                                  (.countDown latch))
                                          :handle-consume-ok-fn (fn [consumer-tag]
                                                                  (.countDown latch)))]
    (lhb/consume channel queue consumer)
    (.start (Thread. (fn []
                       (dotimes [i n]
                         (lhb/publish channel exchange queue payload))) "publisher"))
    (.await latch)))

(deftest t-shutdown-notification-handler
  (let [ch    (lch/open conn)
        q     (:queue (lhq/declare ch))
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default ch :handle-shutdown-signal-fn (fn [consumer_tag reason]
                                                                        (.countDown latch)))]
    (lhb/consume ch q consumer)
    (.start (Thread. (fn []
                       (Thread/sleep 200)
                       ;; bind a queue that does not exist
                       (try
                         (lhq/bind ch "ugggggh" "amq.fanout")
                         (catch Exception e
                           (comment "Do nothing"))))))
    (.await latch)))
