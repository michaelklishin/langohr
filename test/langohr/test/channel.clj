(ns langohr.test.channel
  (:import (com.rabbitmq.client Connection Channel))
  (:use [clojure.test])
  (:require [langohr.core    :as lhcore]
            [langohr.channel :as lhch]))

(deftest t-open-a-channel
  (let [conn (lhcore/connect)
        ch   (.createChannel conn)]
    (is (instance? com.rabbitmq.client.Channel ch))
    (is (lhcore/open? ch))))

(deftest t-open-a-channel-with-explicitly-given-id
  (let [conn (lhcore/connect)
        ch   (.createChannel conn 987)]
    (is (instance? com.rabbitmq.client.Channel ch))
    (is (lhcore/open? ch))
    (is (= (.getChannelNumber ch) 987))
    (lhcore/close ch)))


(deftest t-close-a-channel-using-langohr-core-close
  (let [conn (lhcore/connect)
        ch   (.createChannel conn)]
    (is (lhcore/open? ch))
    (lhcore/close ch)
    (is (not (lhcore/open? ch)))))

(deftest t-close-a-channel-using-langohr-channel-close
  (let [conn (lhcore/connect)
        ch   (.createChannel conn)]
    (is (lhcore/open? ch))
    (lhch/close ch)
    (is (not (lhcore/open? ch)))))


(deftest t-toggle-flow-control
  (let [conn (lhcore/connect)
        ch   (.createChannel conn)]
    (is (lhch/flow? ch))
    (lhch/flow ch false)
    (lhch/flow ch true)
    (is (lhch/flow? ch))
    (lhch/close ch)
    (is (not (lhch/open? ch)))))
