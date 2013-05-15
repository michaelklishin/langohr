(ns langohr.test.shutdown-test
  (:import [com.rabbitmq.client Connection Channel Consumer ShutdownSignalException])
  (:require [langohr.queue     :as lhq]
            [langohr.core      :as lhc]
            [langohr.channel   :as lch]
            [langohr.basic     :as lhb]
            [langohr.consumers :as lhcons]
            [langohr.shutdown  :as lh])
  (:use clojure.test))


(defonce ^Connection conn (lhc/connect))

(deftest test-channel-of-with-a-channel-exception
  (let [ch    (lch/open conn)
        q     (:queue (lhq/declare ch))
        latch    (java.util.concurrent.CountDownLatch. 1)
        cha      (atom nil)
        f        (fn [consumer_tag ^ShutdownSignalException reason]
                   (reset! cha (lh/channel-of reason))
                   (.countDown latch))
        consumer (lhcons/create-default ch :handle-shutdown-signal-fn f)]
    (lhb/consume ch q consumer)
    (.start (Thread. (fn []
                       (Thread/sleep 200)
                       ;; bind a queue that does not exist
                       (try
                         (lhq/bind ch "ugggggh" "amq.fanout")
                         (catch Exception e
                           (comment "Do nothing"))))))
    (.await latch)
    (is (= @cha ch))))

(deftest test-connection-of
  (let [ch    (lch/open conn)
        q     (:queue (lhq/declare ch))
        latch    (java.util.concurrent.CountDownLatch. 1)
        conn'    (atom nil)
        f        (fn [consumer_tag ^ShutdownSignalException reason]
                   (reset! conn' (lh/connection-of reason))
                   (.countDown latch))
        consumer (lhcons/create-default ch :handle-shutdown-signal-fn f)]
    (lhb/consume ch q consumer)
    (.start (Thread. (fn []
                       (Thread/sleep 200)
                       (try
                         (lhq/bind ch "ugggggh" "amq.fanout")
                         (catch Exception e
                           (comment "Do nothing"))))))
    (.await latch)
    (is (= @conn' conn))))
