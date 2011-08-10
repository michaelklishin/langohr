(set! *warn-on-reflection* true)

(ns langohr.test.basic
  (:import (com.rabbitmq.client Connection Channel AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder QueueingConsumer GetResponse QueueingConsumer$Delivery)
           (java.security SecureRandom) (java.math.BigInteger))
  (:use [clojure.test] [langohr.core :as lhc] [langohr.queue :as lhq] [langohr.basic :as lhb]))

;;
;; basic.publish, basic.consume
;;

(defonce ^:dynamic ^Connection *conn* (lhc/connect))


(deftest t-publishing-using-default-exchange-and-default-message-attributes-take-1
  (let [channel    (.createChannel *conn*)
        exchange   ""
        ;; yes, payload may be blank. This is an edge case Ruby amqp
        ;; gem did not support for a long time so I want to use it in the langohr
        ;; test suite. MK.
        payload    ""
        queue        "langohr.examples.publishing.using-default-exchange"
        declare-ok   (lhq/declare channel queue { :auto-delete true })
        monitor      (Object.)
        consumer-tag (.toString (new BigInteger 130 (SecureRandom.)) 32)]
    (do
      (lhb/publish channel payload { :routing-key queue, :exchange exchange })
      (lhb/consume channel queue #((.notify monitor)) { :consumer-tag consumer-tag, :auto-ack true })
      (.wait monitor))))



;;
;; basic.get
;;

(deftest t-basic-get-with-automatic-ack
  (let [channel    (.createChannel *conn*)
        exchange   ""
        payload    "A message we will fetch with basic.get"
        queue      "langohr.examples.basic.get.queue1"
        declare-ok (lhq/declare channel queue { :auto-delete true })]
    (lhb/publish channel payload { :routing-key queue, :exchange exchange })
    (let [get-response (lhb/get channel queue)]
      (is (instance? GetResponse get-response))
      (is (= (String. (.getBody get-response)) payload))
      (is (= (.getMessageCount get-response) 0))
      (is (= (.. get-response getEnvelope getExchange) exchange))
      (is (= (.. get-response getEnvelope getRoutingKey) queue)))))

(deftest t-basic-get-with-explicit-ack
  (let [channel    (.createChannel *conn*)
        exchange   ""
        payload    "A message we will fetch with basic.get"
        queue      "langohr.examples.basic.get.queue1"
        declare-ok (lhq/declare channel queue { :auto-delete true })]
    (lhb/publish channel payload { :routing-key queue, :exchange exchange })
    (let [get-response (lhb/get channel queue false)]
      (is (instance? GetResponse get-response))
      (is (= (String. (.getBody get-response)) payload)))))





;;
;; basic.qos
;;

(deftest t-using-non-global-basic-qos
  (let [channel (.createChannel *conn*)]
    (lhb/qos channel 5)))

