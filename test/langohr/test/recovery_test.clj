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
    (Thread/sleep 2000)
    (is (rmq/open? ch1))
    (is (rmq/open? ch2))))
