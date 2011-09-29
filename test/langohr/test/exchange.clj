(set! *warn-on-reflection* true)

(ns langohr.test.exchange
  (:refer-clojure :exclude [declare])
  (:import (com.rabbitmq.client Connection Channel AMQP  AMQP$Exchange$DeclareOk AMQP$Exchange$DeleteOk AMQP$Queue$DeclareOk ShutdownSignalException)
           (java.io IOException))
  (:use     [clojure.test])
  (:require [langohr.core     :as lhc]
            [langohr.exchange :as lhe]
            [langohr.queue    :as lhq]
            [langohr.basic    :as lhb]))


;;
;; exchange.declare
;;

(defonce conn (lhc/connect))


(deftest t-declare-a-direct-exchange-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct1"]
    (lhe/declare channel exchange "direct")))


(deftest t-declare-a-durable-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct2"]
    (lhe/declare channel exchange "direct" :auto-delete false, :durable true)))


(deftest t-declare-an-auto-deleted-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct3"]
    (lhe/declare channel exchange "direct" :auto-delete true, :durable false)))




(deftest t-declare-a-fanout-exchange-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout1"]
    (lhe/declare channel exchange "fanout")))


(deftest t-declare-a-durable-fanout-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout2"]
    (lhe/declare channel exchange "fanout" :durable true)))


(deftest t-declare-an-auto-deleted-fanout-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout3"]
    (lhe/declare channel exchange "fanout" :auto-delete true)))




(deftest t-declare-a-topic-exchange-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic1"]
    (lhe/declare channel exchange "topic")))


(deftest t-declare-a-durable-topic-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic2"]
    (lhe/declare channel exchange "topic" :durable true)))


(deftest t-declare-an-auto-deleted-topic-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic3"]
    (lhe/declare channel exchange "topic" :auto-delete true)))


(deftest t-redeclare-an-auto-deleted-topic-exchange-with-different-attributes
  (let [conn        (lhc/connect)
        channel     (lhc/create-channel conn)
        exchange    "langohr.tests.exchanges.topic4"
        shutdown-ln (lhc/shutdown-listener (fn [cause]
                                             (println "Shutdown listener has fired")))]
    (try
      (.addShutdownListener channel shutdown-ln)
      (lhe/declare channel exchange "topic" :auto-delete true)
      (lhe/declare channel exchange "topic" :auto-delete false)
      (catch IOException ioe ;; see http://www.rabbitmq.com/api-guide.html#shutdown
        nil))))



;;
;; exchange.delete
;;

(deftest t-delete-a-fresh-declared-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct4"]
    (lhe/declare channel exchange "direct")
    (lhe/delete  channel exchange)))

(deftest t-delete-a-fresh-declared-direct-exchange-if-it-is-unused
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct5"]
    (lhe/declare channel exchange "direct")
    (lhe/delete  channel exchange true)))



;;
;; exchange.bind
;;

(deftest t-exchange-bind-without-arguments
  (let [channel     (lhc/create-channel conn)
        source      "langohr.tests.exchanges.source"
        destination "langohr.tests.exchanges.destination"
        queue       (.getQueue (lhq/declare channel "" :auto-delete true))]
    (lhe/declare channel source      "fanout" :auto-delete true)
    (lhe/declare channel destination "fanout" :auto-delete true)
    (lhq/bind channel queue destination)
    (is (nil? (lhb/get channel queue)))
    (lhe/bind    channel destination source)
    (lhb/publish channel source "" "")
    (is (lhb/get channel queue))))

(deftest t-exchange-bind-with-arguments
  (let [channel     (lhc/create-channel conn)
        source      "langohr.tests.exchanges.source2"
        destination "langohr.tests.exchanges.destination2"
        queue       (.getQueue (lhq/declare channel "" :auto-delete true))]
    (lhe/declare channel source      "fanout" :auto-delete true)
    (lhe/declare channel destination "fanout" :auto-delete true)
    (lhq/bind channel queue destination :arguments { "X-For-Some-Extension" "a value" })
    (is (nil? (lhb/get channel queue)))
    (lhe/bind    channel destination source)
    (lhb/publish channel source "" "")
    (is (lhb/get channel queue))))
