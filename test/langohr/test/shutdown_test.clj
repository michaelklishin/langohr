(ns langohr.test.shutdown-test
  (:require [langohr.queue     :as lhq]
            [langohr.core      :as lhc]
            [langohr.channel   :as lch]
            [langohr.basic     :as lhb]
            [langohr.consumers :as lhcons]
            [langohr.shutdown  :as lh]
            [clojure.test      :refer :all])
  (:import [com.rabbitmq.client Connection Channel Consumer ShutdownSignalException]
           java.util.concurrent.TimeUnit))


(deftest test-channel-of-with-a-channel-exception
  (with-open [^Connection conn (lhc/connect)]
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
                         (try
                           (lhq/bind ch "ugggggh" "amq.fanout")
                           (catch Exception e
                             (comment "Do nothing"))))))
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (= @cha (.getDelegate ch))))))

(deftest test-connection-of
  (with-open [^Connection conn (lhc/connect)]
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
                         (try
                           (lhq/bind ch "ugggggh" "amq.fanout")
                           (catch Exception e
                             (comment "Do nothing"))))))
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (= @conn' (.getDelegate conn))))))

(deftest test-initiator-with-a-channel-exception
  (with-open [^Connection conn (lhc/connect)]
    (let [ch    (lch/open conn)
          q     (:queue (lhq/declare ch))
          latch    (java.util.concurrent.CountDownLatch. 1)
          sse      (atom nil)
          f        (fn [consumer_tag ^ShutdownSignalException reason]
                     (reset! sse reason)
                     (.countDown latch))
          consumer (lhcons/create-default ch :handle-shutdown-signal-fn f)]
      (lhb/consume ch q consumer)
      (.start (Thread. (fn []
                         (try
                           (lhq/bind ch "ugggggh" "amq.fanout")
                           (catch Exception e
                             (comment "Do nothing"))))))
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (lh/initiated-by-broker? @sse))
      (is (not (lh/initiated-by-application? @sse))))))

(deftest test-initiator-with-an-explicit-channel-closure
  (with-open [^Connection conn (lhc/connect)]
    (let [ch    (lch/open conn)
          q     (:queue (lhq/declare ch))
          latch    (java.util.concurrent.CountDownLatch. 1)
          sse      (atom nil)
          f        (fn [consumer_tag ^ShutdownSignalException reason]
                     (reset! sse reason)
                     (.countDown latch))
          consumer (lhcons/create-default ch :handle-shutdown-signal-fn f)]
      (lhb/consume ch q consumer)
      (Thread/sleep 250)
      (lhc/close ch)
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (not (lh/initiated-by-broker? @sse)))
      (is (lh/initiated-by-application? @sse)))))

(deftest test-initiator-with-an-unhandled-consumer-exception
  (with-open [^Connection conn (lhc/connect)]
    (let [ch    (lch/open conn)
          q     (:queue (lhq/declare ch))
          latch    (java.util.concurrent.CountDownLatch. 1)
          sse      (atom nil)
          dhf      (fn [ch metadata payload]
                     ;; something terrible happens
                     (throw (RuntimeException. "the monster, it is out! Run for life!")))
          ssf      (fn [consumer_tag ^ShutdownSignalException reason]
                     (reset! sse reason)
                     (.countDown latch))
          consumer (lhcons/create-default ch :handle-delivery-fn dhf :handle-shutdown-signal-fn ssf)]
      (lhb/consume ch q consumer)
      (Thread/sleep 250)
      (lhb/publish ch "" q "message")
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (not (lh/initiated-by-broker? @sse)))
      (is (lh/initiated-by-application? @sse)))))
