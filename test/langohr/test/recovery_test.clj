(ns langohr.test.recovery-test
  "Connection recovery tests"
  (:refer-clojure :exclude [await])
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

(def ^:const expected-recovery-period 500)
(def ^:const recovery-delay           200)
(defn wait-for-recovery
  []
  (Thread/sleep expected-recovery-period))

(defn close-all-connections
  []
  (doseq [x (map :name (mgmt/list-connections))]
    (mgmt/close-connection x)))

(defn ensure-queue-recovery
  [ch ^String q]
  (lb/publish ch "" q "a message")
  (Thread/sleep 50)
  (is (not (lq/empty? ch q))))

(defn await-on
  ([^CountDownLatch latch]
     (is (.await latch 500 TimeUnit/MILLISECONDS)))
  ([^CountDownLatch latch ^long n ^TimeUnit tu]
     (is (.await latch n tu))))

;;
;; Tests
;;

(deftest test-basic-connection-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay recovery-delay})]
    (is (rmq/open? conn))
    (close-all-connections)
    (Thread/sleep 100)
    (is (not (rmq/open? conn)))
    ;; wait for recovery to finish
    (wait-for-recovery)
    (is (rmq/open? conn))))


(deftest test-basic-channel-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay recovery-delay})]
    (let [ch1  (lch/open conn)
          ch2  (lch/open conn)]
      (is (rmq/open? ch1))
      (is (rmq/open? ch2))
      (close-all-connections)
      (Thread/sleep 50)
      (is (not (rmq/open? ch1)))
      (is (not (rmq/open? ch2)))
      ;; wait for recovery to finish
      (wait-for-recovery)
      (is (rmq/open? ch1))
      (is (rmq/open? ch2)))))

(deftest test-basic-client-named-queue-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch   (lch/open conn)
          q    "langohr.test.recovery.q1"]
      (lq/declare ch q :durable true)
      (lq/purge ch q)
      (is (lq/empty? ch q))
      (close-all-connections)
      (wait-for-recovery)
      (is (rmq/open? ch))
      (ensure-queue-recovery ch q))))

(deftest test-basic-server-named-queue-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
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
      (wait-for-recovery)
      (is (rmq/open? ch))
      (lb/publish ch x "test-basic-server-named-queue-recovery" "a message")
      (await-on latch))))

(deftest test-server-named-queue-recovery-with-multiple-queues
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
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
      (wait-for-recovery)
      (is (rmq/open? ch))
      (lb/publish ch x "" "a message")
      (await-on latch))))

(deftest test-e2e-binding-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch   (lch/open conn)
          x1   "langohr.test.recovery.fanout1"
          x2   "langohr.test.recovery.fanout2"
          latch (CountDownLatch. 1)
          f     (fn [_ _ _]
                  (.countDown latch))
          q     (lq/declare-server-named ch :exclusive true)]
      (lx/fanout ch x1 :durable true)
      (lx/fanout ch x2 :durable true)
      (lx/bind ch x2 x1)
      (lq/bind ch q x2)
      (lc/subscribe ch q f)
      (is (lq/empty? ch q))
      (close-all-connections)
      (wait-for-recovery)
      (is (rmq/open? ch))
      (lb/publish ch x1 "" "a message")
      (await-on latch))))

;; q1 => q2 => ... => q(n-1) => q(n)
(deftest test-merry-go-around-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [n     200
          ch    (lch/open conn)
          latch (CountDownLatch. n)
          x     ""
          qs    (for [i (range 0 n)]
                  (format "langohr.test.recovery.merry-go-around.q.%d" i))]
      (dotimes [i n]
        (let [q  (nth qs i)
              nq (try
                   (nth qs (inc i))
                   (catch IndexOutOfBoundsException oob
                     nil))
              f  (fn [_ _ ^bytes payload]
                   #_ (println (format "Received %s" (String. payload "UTF-8")))
                   (when nq
                     (lb/publish ch x nq (format "message.%d" i)))
                   (.countDown latch))]
          #_ (println (format "Declaring queue %s" q))
          (lq/declare ch q :exclusive true)
          (lc/subscribe ch q f)))
      (close-all-connections)
      (wait-for-recovery)
      (is (rmq/open? ch))
      (lb/publish ch x (first qs) "a message")
      (await-on latch 10 TimeUnit/SECONDS))))
