(ns langohr.test.tx-test
  (:import [com.rabbitmq.client Connection AMQP$Tx$SelectOk AMQP$Tx$CommitOk AMQP$Tx$RollbackOk])
  (:require langohr.tx
            [langohr.core :as lhc])
  (:use clojure.test))

(defonce ^Connection conn (lhc/connect))

(deftest t-tx-select-is-isolation
  (let [channel (lhc/create-channel conn)]
    (is (instance? AMQP$Tx$SelectOk (langohr.tx/select channel)))))

(deftest t-tx-commit-is-isolation
  (let [channel (lhc/create-channel conn)]
    (is (instance? AMQP$Tx$SelectOk (langohr.tx/select channel)))
    (is (instance? AMQP$Tx$CommitOk (langohr.tx/commit channel)))))

(deftest t-tx-rollback-is-isolation
  (let [channel (lhc/create-channel conn)]
    (is (instance? AMQP$Tx$SelectOk   (langohr.tx/select channel)))
    (is (instance? AMQP$Tx$RollbackOk (langohr.tx/rollback channel)))))
