(ns langohr.test.recovery-test
  "Connection recovery tests"
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as lx]
            [langohr.basic     :as lb]
            [langohr.consumers :as lc]
            [clojure.test :refer :all]
            [langohr.http      :as mgmt])
  (:import [java.util.concurrent CountDownLatch
            TimeUnit]))

;;
;; Helpers
;;

(defn close-all-connections
  []
  (doseq [x (map :name (mgmt/list-connections))]
    (mgmt/close-connection x)))

(defn ensure-queue-recovery
  [ch ^String q]
  (lb/publish ch "" q "a message")
  (Thread/sleep 50)
  (is (not (lq/empty? ch q))))

(defn await
  [^CountDownLatch latch]
  (is (.await latch 500 TimeUnit/MILLISECONDS)))

;;
;; Tests
;;

(deftest test-basic-connection-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay 500})]
    (is (rmq/open? conn))
    (close-all-connections)
    (Thread/sleep 200)
    (is (not (rmq/open? conn)))
    ;; wait for recovery to finish
    (Thread/sleep 800)
    (is (rmq/open? conn))))


(deftest test-basic-channel-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay 500})]
    (let [ch1  (lch/open conn)
          ch2  (lch/open conn)]
      (is (rmq/open? ch1))
      (is (rmq/open? ch2))
      (close-all-connections)
      (Thread/sleep 200)
      (is (not (rmq/open? ch1)))
      (is (not (rmq/open? ch2)))
      ;; wait for recovery to finish
      (Thread/sleep 800)
      (is (rmq/open? ch1))
      (is (rmq/open? ch2)))))

(deftest test-basic-client-named-queue-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay 500})]
    (let [ch   (lch/open conn)
          q    "langohr.test.recovery.q1"]
      (lq/declare ch q :durable true)
      (lq/purge ch q)
      (is (lq/empty? ch q))
      (close-all-connections)
      (Thread/sleep 800)
      (is (rmq/open? ch))
      (ensure-queue-recovery ch q))))

(deftest test-basic-server-named-queue-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay 500})]
    (let [ch   (lch/open conn)
          x    "langohr.test.recovery.direct1"
          latch (CountDownLatch. 1)
          f     (fn [_ _ _]
                  (.countDown latch))
          q     (lq/declare-server-named ch :exclusive true)]
      (lx/direct ch x :durable true)
      (lq/bind ch q x :routing-key "test-basic-server-named-queue-recovery")
      (lc/subscribe ch q f)
      (is (lq/empty? ch q))
      (close-all-connections)
      (Thread/sleep 800)
      (is (rmq/open? ch))
      (lb/publish ch x "test-basic-server-named-queue-recovery" "a message")
      (await latch))))

(deftest test-server-named-queue-recovery-with-multiple-queues
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay 500})]
    (let [ch   (lch/open conn)
          x    "langohr.test.recovery.fanout1"
          latch (CountDownLatch. 2)
          f     (fn [_ _ _]
                  (.countDown latch))
          q1    (lq/declare-server-named ch :exclusive true)
          q2    (lq/declare-server-named ch :exclusive true)]
      (lx/fanout ch x :durable true)
      (lq/bind ch q1 x)
      (lq/bind ch q2 x)
      (lc/subscribe ch q1 f)
      (lc/subscribe ch q2 f)
      (is (lq/empty? ch q1))
      (is (lq/empty? ch q2))
      (close-all-connections)
      (Thread/sleep 800)
      (is (rmq/open? ch))
      (lb/publish ch x "" "a message")
      (await latch))))
