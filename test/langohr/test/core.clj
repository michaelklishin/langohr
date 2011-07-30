(set! *warn-on-reflection* true)

(ns langohr.test.core
  (:import (com.rabbitmq.client Connection Channel))
  (:use [clojure.test] [langohr.core]))

(deftest t-connection-with-default-parameters
  (let [conn (connect)]
    (is (instance? com.rabbitmq.client.Connection conn))
    (is (open? conn))))

(deftest t-close-connection
  (let [conn (connect)]
    (is (open? conn))
    (close conn)
    (is (not (open? conn)))))

(deftest t-open-a-channel
  (let [conn (connect)
        ch   (.createChannel conn)]
    (is (instance? com.rabbitmq.client.Channel ch))
    (is (open? ch))))

(deftest t-open-a-channel-with-explicitly-given-id
  (let [conn (connect)
        ch   (.createChannel conn 987)]
    (is (instance? com.rabbitmq.client.Channel ch))
    (is (open? ch))
    (is (= (.getChannelNumber ch) 987))
    (close ch)))


(deftest t-close-a-channel
  (let [conn (connect)
        ch   (.createChannel conn)]
    (is (open? ch))
    (close ch)
    (is (not (open? ch)))))
