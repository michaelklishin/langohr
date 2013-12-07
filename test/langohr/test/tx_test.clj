(ns langohr.test.tx-test
  (:import [com.rabbitmq.client Connection AMQP$Tx$SelectOk AMQP$Tx$CommitOk AMQP$Tx$RollbackOk])
  (:require langohr.tx
            [langohr.core :as lhc]
            [clojure.test :refer :all]))

(deftest t-tx-select-in-isolation
  (with-open [^Connection conn (lhc/connect)]
    (let [ch (lhc/create-channel conn)]
      (is (instance? AMQP$Tx$SelectOk (langohr.tx/select ch))))))

(deftest t-tx-commit-in-isolation
  (with-open [^Connection conn (lhc/connect)]
    (let [ch (lhc/create-channel conn)]
      (is (instance? AMQP$Tx$SelectOk (langohr.tx/select ch)))
      (is (instance? AMQP$Tx$CommitOk (langohr.tx/commit ch))))))

(deftest t-tx-rollback-in-isolation
  (with-open [^Connection conn (lhc/connect)]
    (let [ch (lhc/create-channel conn)]
      (is (instance? AMQP$Tx$SelectOk   (langohr.tx/select ch)))
      (is (instance? AMQP$Tx$RollbackOk (langohr.tx/rollback ch))))))
