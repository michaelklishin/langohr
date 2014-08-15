;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.recovery-test
  "Connection recovery tests"
  (:refer-clojure :exclude [await])
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as lx]
            [langohr.basic     :as lb]
            [langohr.consumers :as lc]
            [langohr.confirm   :as lcnf]
            [clojure.test :refer :all]
            [langohr.http      :as mgmt])
  (:import [java.util.concurrent CountDownLatch
            TimeUnit]
           java.util.UUID))

;;
;; Helpers
;;

(def ^:const expected-recovery-period 500)
(def ^:const recovery-delay           200)
(defn wait-for-recovery
  [conn]
  (let [latch (CountDownLatch. 1)]
    (rmq/on-recovery conn (fn [_]
                            (.countDown latch)))
    (.await latch (+ expected-recovery-period 250) TimeUnit/MILLISECONDS)))

(defn close-all-connections
  []
  (doseq [x (map :name (mgmt/list-connections))]
    (mgmt/close-connection x)))

(defn ensure-queue-recovery
  [ch ^String q]
  (lb/publish ch "" q "a message")
  (Thread/sleep 75)
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
    (is (rmq/automatic-recovery-enabled? conn))
    (is (rmq/open? conn))
    (close-all-connections)
    (Thread/sleep 100)
    (is (not (rmq/open? conn)))
    ;; wait for recovery to finish
    (wait-for-recovery conn)
    (is (rmq/open? conn))))

(deftest test-connection-recovery-with-disabled-topology-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay recovery-delay})]
    (is (rmq/automatic-recovery-enabled? conn))
    (is (not (rmq/automatic-topology-recovery-enabled? conn)))
    (let [ch (lch/open conn)
          q  (lq/declare-server-named ch)]
      (is (rmq/open? ch))
      (lq/declare-passive ch q)
      (close-all-connections)
      (Thread/sleep 100)
      (wait-for-recovery conn)
      (is (rmq/open? ch))
      (is (thrown? java.io.IOException
                   (lq/declare-passive ch q))))))

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
      (wait-for-recovery conn)
      (is (rmq/open? ch1))
      (is (rmq/open? ch2)))))

(deftest test-shutdown-hooks-recovery-on-connection
  (let [conn  (rmq/connect {:automatically-recover true
                            :automatically-recover-topology false
                            :network-recovery-delay recovery-delay})
        latch (CountDownLatch. 2)]
    (rmq/add-shutdown-listener conn (fn [_]
                                      (.countDown latch)))
    (close-all-connections)
    (Thread/sleep 50)
    (is (not (rmq/open? conn)))
    ;; wait for recovery to finish
    (wait-for-recovery conn)
    (is (rmq/open? conn))
    (rmq/close conn)
    (await-on latch 10 TimeUnit/SECONDS)))

(deftest test-shutdown-hooks-recovery-on-channel
  (let [conn  (rmq/connect {:automatically-recover true
                            :automatically-recover-topology false
                            :network-recovery-delay recovery-delay})
        latch (CountDownLatch. 3)
        ch    (lch/open conn)]
    (rmq/add-shutdown-listener ch (fn [_]
                                    (.countDown latch)))
    (close-all-connections)
    (Thread/sleep 50)
    (is (not (rmq/open? conn)))
    (wait-for-recovery conn)
    (is (rmq/open? conn))
    (close-all-connections)
    (wait-for-recovery conn)
    (is (rmq/open? conn))
    (rmq/close conn)
    (await-on latch 10 TimeUnit/SECONDS)))

(deftest test-publisher-confirms-state-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay recovery-delay})]
    (let [ch  (lch/open conn)
          q   (lq/declare-server-named ch)]
      (lcnf/select ch)
      (is (rmq/open? ch))
      (close-all-connections)
      ;; wait for recovery to finish
      (wait-for-recovery conn)
      (is (rmq/open? ch))
      (lb/publish ch "" q "a message")
      (lcnf/wait-for-confirms ch))))

(deftest test-basic-client-named-queue-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch   (lch/open conn)
          q    "langohr.test.recovery.q1"]
      (is (rmq/automatic-recovery-enabled? conn))
      (is (rmq/automatic-topology-recovery-enabled? conn))
      (lq/declare ch q :durable true)
      (lq/purge ch q)
      (is (lq/empty? ch q))
      (close-all-connections)
      (wait-for-recovery conn)
      (is (rmq/open? ch))
      (ensure-queue-recovery ch q))))

(deftest test-manual-client-named-queue-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology false
                                 :network-recovery-delay recovery-delay})]
    (let [ch    (lch/open conn)
          q     (str (UUID/randomUUID))
          ctag  (str (UUID/randomUUID))
          latch (CountDownLatch. 1)
          hf    (fn [ch meta ^bytes payload]
                  (.countDown latch))
          f     (fn [ch q]
                  (lq/declare ch q :durable false)
                  (lq/purge ch q)
                  (lc/subscribe ch q hf :auto-ack true :consumer-tag ctag))]
      (is (rmq/automatic-recovery-enabled? conn))
      (is (not (rmq/automatic-topology-recovery-enabled? conn)))
      (f ch q)
      (rmq/on-recovery ch
                       (fn [ch]
                         (f ch q)))
      (close-all-connections)
      (wait-for-recovery conn)
      (is (rmq/open? ch))
      ;; wait a bit more to make sure the consumer is there
      (Thread/sleep 150)
      (lb/publish ch "" q "a message")
      (is (await-on latch 15 TimeUnit/SECONDS))
      (lq/delete ch q))))


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
      (wait-for-recovery conn)
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
      (wait-for-recovery conn)
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
      (wait-for-recovery conn)
      (is (rmq/open? ch))
      (lb/publish ch x1 "" "a message")
      (await-on latch))))

(deftest test-removed-e2e-bindings-do-not-reappear-after-recovery
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
      (lx/unbind ch x2 x1)
      (lc/subscribe ch q f)
      (is (lq/empty? ch q))
      (close-all-connections)
      (wait-for-recovery conn)
      (is (rmq/open? ch))
      (lb/publish ch x1 "" "a message")
      (is (not (.await latch 100 TimeUnit/MILLISECONDS))))))

(deftest test-removed-e2e-bindings-declared-and-deleted-on-separate-channels-do-not-reappear-after-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch1  (lch/open conn)
          ch2  (lch/open conn)
          x1   "langohr.test.recovery.fanout1"
          x2   "langohr.test.recovery.fanout2"
          latch (CountDownLatch. 1)
          f     (fn [_ _ _]
                  (.countDown latch))
          q     (lq/declare-server-named ch1 :exclusive true)]
      (lx/fanout ch1 x1 :durable true)
      (lx/fanout ch1 x2 :durable true)
      (lx/bind ch1 x2 x1)
      (lq/bind ch2 q x2)
      (lx/unbind ch2 x2 x1)
      (lc/subscribe ch1 q f)
      (is (lq/empty? ch2 q))
      (close-all-connections)
      (wait-for-recovery conn)
      (is (rmq/open? ch1))
      (is (rmq/open? ch2))
      (lb/publish ch1 x1 "" "a message")
      (is (not (.await latch 100 TimeUnit/MILLISECONDS))))))

(deftest test-queue-binding-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch    (lch/open conn)
          x     "langohr.test.recovery.fanout1"
          q     "langohr.test.recovery.q1"
          latch (CountDownLatch. 1)
          f     (fn [_ _ _]
                  (.countDown latch))]
      (lx/fanout ch x :durable true)
      (lq/declare ch q :durable true)
      (lq/purge ch q)
      (lq/bind ch q x)
      (close-all-connections)
      (wait-for-recovery conn)
      (lc/subscribe ch q f)
      (close-all-connections)
      (wait-for-recovery conn)
      (lb/publish ch x "" "a message")
      (is (.await latch 100 TimeUnit/MILLISECONDS))
      (lx/delete ch x))))

(deftest test-removed-queue-binding-does-not-reappear-after-recovery
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch    (lch/open conn)
          x     "langohr.test.recovery.fanout1"
          q     "langohr.test.recovery.q1"
          latch (CountDownLatch. 1)
          f     (fn [_ _ _]
                  (.countDown latch))]
      (lx/fanout ch x :durable true)
      (lq/declare ch q :durable true)
      (lq/purge ch q)
      (lq/bind ch q x)
      (lq/unbind ch q x)
      (close-all-connections)
      (wait-for-recovery conn)
      (lc/subscribe ch q f)
      (close-all-connections)
      (wait-for-recovery conn)
      (lb/publish ch x "" "a message")
      (is (not (.await latch 100 TimeUnit/MILLISECONDS)))
      (lx/delete ch x))))

(deftest test-recovery-of-all-consumers
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch    (lch/open conn)
          q     "langohr.test.recovery.q1"
          n     1024]
      (lq/declare ch q :durable true)
      (dotimes [i n]
        (lc/subscribe ch q (fn [_ _ _] )))
      (close-all-connections)
      (wait-for-recovery conn)
      (is (= n (lq/consumer-count ch q)))
      (lq/delete ch q))))

(deftest test-recovery-of-all-queues
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :automatically-recover-topology true
                                 :network-recovery-delay recovery-delay})]
    (let [ch    (lch/open conn)
          qs    (atom [])
          n     64]
      (dotimes [i n]
        (let [q (str (UUID/randomUUID))]
          (lq/declare ch q :durable false :exclusive true)
          (swap! qs conj q)))
      (close-all-connections)
      (wait-for-recovery conn)
      (doseq [q @qs]
        (lq/declare-passive ch q)
        (lq/delete ch q)))))

(deftest test-connection-recovery-callback
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :network-recovery-delay recovery-delay})]
    (let [latch (CountDownLatch. 2)
          f     (fn [_]
                  (.countDown latch))]
      (rmq/on-recovery conn f)
      (close-all-connections)
      (wait-for-recovery conn)
      (close-all-connections)
      (wait-for-recovery conn)
      (is (.await latch 100 TimeUnit/MILLISECONDS)))))

(deftest test-channel-recovery-callback
  (with-open [conn (rmq/connect {:automatically-recover true
                                 :network-recovery-delay recovery-delay})]
    (let [ch    (lch/open conn)
          latch (CountDownLatch. 2)
          f     (fn [_]
                  (.countDown latch))]
      (rmq/on-recovery ch f)
      (close-all-connections)
      (wait-for-recovery conn)
      (close-all-connections)
      (wait-for-recovery conn)
      (is (.await latch 100 TimeUnit/MILLISECONDS)))))

(when-not (System/getenv "CI")
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
        (wait-for-recovery conn)
        (is (rmq/open? ch))
        (lb/publish ch x (first qs) "a message")
        (await-on latch 15 TimeUnit/SECONDS)))))

(deftest test-default-topology-recovery-value
  (with-open [conn (rmq/connect {:host "localhost"})]
    (is (= true (rmq/automatic-topology-recovery-enabled? conn)))))
