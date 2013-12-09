(ns langohr.test.recovery-test
  "Connection recovery tests"
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as lx]
            [langohr.basic     :as lb]
            [langohr.consumers :as lc]
            [clojure.test :refer :all]
            [langohr.http      :as mgmt]))

(defn close-all-connections
  []
  (doseq [x (map :name (mgmt/list-connections))]
    (mgmt/close-connection x)))

(deftest test-basic-connection-recovery
  (let [conn (rmq/connect {:automatically-recover true
                           :automatically-recover-topology false
                           :network-recovery-delay 500})]
    (is (rmq/open? conn))
    (close-all-connections)
    (Thread/sleep 200)
    (is (not (rmq/open? conn)))
    ;; wait for recovery to finish
    (Thread/sleep 1000)
    (is (rmq/open? conn))
    (rmq/close conn)))


(deftest test-basic-channel-recovery
  (let [conn (rmq/connect {:automatically-recover true
                           :automatically-recover-topology false
                           :network-recovery-delay 500})
        ch1  (lch/open conn)
        ch2  (lch/open conn)]
    (is (rmq/open? ch1))
    (is (rmq/open? ch2))
    (close-all-connections)
    (Thread/sleep 200)
    (is (not (rmq/open? ch1)))
    (is (not (rmq/open? ch2)))
    ;; wait for recovery to finish
    (Thread/sleep 1000)
    (is (rmq/open? ch1))
    (is (rmq/open? ch2))
    (rmq/close conn)))

(deftest test-basic-client-named-queue-recovery
  (let [conn (rmq/connect {:automatically-recover true
                           :automatically-recover-topology true
                           :network-recovery-delay 500})
        ch   (lch/open conn)
        q    "langohr.test.recovery.q1"]
    (lq/declare ch q :durable true)
    (lq/purge ch q)
    (is (lq/empty? ch q))
    (close-all-connections)
    (Thread/sleep 1000)
    (is (rmq/open? ch))
    (lb/publish ch "" q "a message")
    (is (not (lq/empty? ch q)))
    (rmq/close conn)))
