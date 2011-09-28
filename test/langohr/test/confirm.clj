(ns langohr.test.confirm
  (:import (com.rabbitmq.client Connection AMQP$Confirm$SelectOk))
  (:require [langohr.confirm]
            [langohr.core :as lhc])
  (:use [clojure.test]))

(defonce ^Connection conn (lhc/connect))

(deftest t-confirm-select
  (let [channel (.createChannel conn)]
    (is (instance? AMQP$Confirm$SelectOk (langohr.confirm/select channel)))))
