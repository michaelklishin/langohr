(ns langohr.test.consumers
  (:import (com.rabbitmq.client Connection Consumer))
  (:require [langohr.queue     :as lhq]
            [langohr.core      :as lhc]
            [langohr.basic     :as lhb]
            [langohr.util      :as lhu]
            [langohr.consumers :as lhcons])
  (:use [clojure.test]))


;;
;; API
;;

(defonce ^:dynamic ^Connection *conn* (lhc/connect))


(deftest t-consume-ok-handler
  (let [channel  (.createChannel *conn*)
        queue    (.getQueue (lhq/declare channel))
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :consume-ok-fn (fn [consumer-tag]
                                                                 (.countDown latch)))]
    (lhb/consume channel queue consumer)
    (.await latch)))


(deftest t-cancel-ok-handler
  (let [channel  (.createChannel *conn*)
        queue    (.getQueue (lhq/declare channel))
        tag      (lhu/generate-consumer-tag "t-cancel-ok-handler")
        latch    (java.util.concurrent.CountDownLatch. 1)
        consumer (lhcons/create-default channel :cancel-ok-fn (fn [consumer-tag]
                                                                 (.countDown latch)))]
    (lhb/consume channel queue consumer :consumer-tag tag)
    (lhb/cancel channel tag)
    (.await latch)))
