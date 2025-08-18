;; Copyright (c) 2011-2025 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.tx-test
  (:import [com.rabbitmq.client Connection AMQP$Tx$SelectOk AMQP$Tx$CommitOk AMQP$Tx$RollbackOk])
  (:require langohr.tx
            [langohr.core :as lhc]
            [clojure.test :refer [deftest is]]))

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
