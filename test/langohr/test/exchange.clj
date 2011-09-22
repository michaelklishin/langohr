(set! *warn-on-reflection* true)

(ns langohr.test.exchange
  (:refer-clojure :exclude [declare])
  (:import (com.rabbitmq.client Connection Channel AMQP  AMQP$Exchange$DeclareOk AMQP$Exchange$DeleteOk))
  (:use     [clojure.test])
  (:require [langohr.core     :as lhc]
            [langohr.exchange :as lhe]
            [langohr.queue    :as lhq]
            [langohr.basic    :as lhb]))


;;
;; exchange.declare
;;

(defonce ^:dynamic ^Connection *conn* (lhc/connect))


(deftest t-declare-a-direct-exchange-with-default-attributes
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.direct1"
        declare-ok (lhe/declare channel exchange "direct")]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))


(deftest t-declare-a-durable-direct-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.direct2"
        declare-ok (lhe/declare channel exchange "direct" :auto-delete false, :durable true)]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))


(deftest t-declare-an-auto-deleted-direct-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.direct3"
        declare-ok (lhe/declare channel exchange "direct" :auto-delete true, :durable false)]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))




(deftest t-declare-a-fanout-exchange-with-default-attributes
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.fanout1"
        declare-ok (lhe/declare channel exchange "fanout")]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))


(deftest t-declare-a-durable-fanout-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.fanout2"
        declare-ok (lhe/declare channel exchange "fanout" :durable true)]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))


(deftest t-declare-an-auto-deleted-fanout-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.fanout3"
        declare-ok (lhe/declare channel exchange "fanout" :auto-delete true)]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))




(deftest t-declare-a-topic-exchange-with-default-attributes
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.topic1"
        declare-ok (lhe/declare channel exchange "topic")]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))


(deftest t-declare-a-durable-topic-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.topic2"
        declare-ok (lhe/declare channel exchange "topic" :durable true)]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))


(deftest t-declare-an-auto-deleted-topic-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.topic3"
        declare-ok (lhe/declare channel exchange "topic" :auto-delete true)]
    (is (instance? AMQP$Exchange$DeclareOk declare-ok))))



;;
;; exchange.delete
;;

(deftest t-delete-a-fresh-declared-direct-exchange
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.direct4"
        declare-ok (lhe/declare channel exchange "direct")
        delete-ok  (lhe/delete  channel exchange)]
    (is (instance? AMQP$Exchange$DeleteOk delete-ok))))

(deftest t-delete-a-fresh-declared-direct-exchange-if-it-is-unused
  (let [^Channel   channel    (lhc/create-channel *conn*)
        ^String    exchange   "langohr.tests.exchanges.direct5"
        declare-ok (lhe/declare channel exchange "direct")
        delete-ok  (lhe/delete  channel exchange true)]
    (is (instance? AMQP$Exchange$DeleteOk delete-ok))))



;;
;; exchange.bind
;;

(deftest t-exchange-bind-without-arguments
  (let [channel     (lhc/create-channel *conn*)
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
