;; Copyright (c) 2011-2025 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.shutdown-test
  (:require [langohr.queue     :as lhq]
            [langohr.core      :as lhc]
            [langohr.channel   :as lch :refer [as-non-recovering-channel]]
            [langohr.basic     :as lhb]
            [langohr.consumers :as lhcons]
            [langohr.shutdown  :as lh]
            [clojure.test      :refer :all])
  (:import [com.rabbitmq.client Connection Channel Consumer ShutdownSignalException]
           [java.util.concurrent CountDownLatch TimeUnit]))


(deftest test-channel-of-with-a-channel-exception
  (with-open [^Connection conn (lhc/connect)]
    (let [ch    (lch/open conn)
          q     (lhq/declare-server-named ch)
          latch    (java.util.concurrent.CountDownLatch. 1)
          cha      (atom nil)
          f        (fn [consumer_tag ^ShutdownSignalException reason]
                     (reset! cha (lh/channel-of reason))
                     (.countDown latch))
          consumer (lhcons/create-default ch {:handle-shutdown-signal-fn f})]
      (lhb/consume ch q consumer)
      (try
        (lhq/bind ch "ugggggh" "amq.fanout")
        (catch Exception e
          (comment "Do nothing")))
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (= @cha (as-non-recovering-channel ch))))))

(deftest test-initiator-with-a-channel-exception
  (with-open [^Connection conn (lhc/connect)]
    (let [ch    (lch/open conn)
          q     (lhq/declare-server-named ch)
          latch    (java.util.concurrent.CountDownLatch. 1)
          sse      (atom nil)
          f        (fn [consumer_tag ^ShutdownSignalException reason]
                     (reset! sse reason)
                     (.countDown latch))
          consumer (lhcons/create-default ch {:handle-shutdown-signal-fn f})]
      (lhb/consume ch q consumer)
      (try
        (lhq/bind ch "ugggggh" "amq.fanout")
        (catch Exception e
          (comment "Do nothing")))
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (lh/initiated-by-broker? @sse))
      (is (not (lh/initiated-by-application? @sse))))))

(deftest test-initiator-with-an-explicit-channel-closure
  (with-open [^Connection conn (lhc/connect)]
    (let [ch    (lch/open conn)
          q     (lhq/declare-server-named ch)
          latch    (java.util.concurrent.CountDownLatch. 1)
          sse      (atom nil)
          f        (fn [consumer_tag ^ShutdownSignalException reason]
                     (reset! sse reason)
                     (.countDown latch))
          consumer (lhcons/create-default ch {:handle-shutdown-signal-fn f})]
      (lhb/consume ch q consumer)
      (Thread/sleep 250)
      (lhc/close ch)
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (not (lh/initiated-by-broker? @sse)))
      (is (lh/initiated-by-application? @sse)))))

(deftest test-custom-exception-handler
  (let [el (CountDownLatch. 1)
        eh (lhc/exception-handler {:handle-consumer-exception-fn (fn [ch ex consumer
                                                                      consumer-tag method-name]
                                                                   (.countDown el))})]
    (with-open [^Connection conn (lhc/connect {:exception-handler eh})]
      (let [ch    (lch/open conn)
            q     (lhq/declare-server-named ch)
            cl    (CountDownLatch. 1)
            dhf      (fn [ch metadata payload]
                       (.countDown cl)
                       (throw (RuntimeException. "the monster, it is out! Run for life!")))]
        (lhcons/subscribe ch q dhf)
        (Thread/sleep 50)
        (lhb/publish ch "" q "message")
        (is (.await cl 700 TimeUnit/MILLISECONDS))
        (is (.await el 700 TimeUnit/MILLISECONDS))))))
