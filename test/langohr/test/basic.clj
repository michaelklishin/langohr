(set! *warn-on-reflection* true)

(ns langohr.test.basic
  (:import (com.rabbitmq.client Connection Channel AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder QueueingConsumer GetResponse))
  (:use [clojure.test] [langohr.core :as lhc] [langohr.queue :as lhq] [langohr.basic :as lhb] [langohr.util :as lhu]))

;;
;; basic.publish, basic.consume
;;

(defonce ^:dynamic ^Connection *conn* (lhc/connect))


(deftest t-publishing-using-default-exchange-and-default-message-attributes
  (let [channel    (.createChannel *conn*)
        exchange   ""
        ;; yes, payload may be blank. This is an edge case Ruby amqp
        ;; gem did not support for a long time so I want to use it in the langohr
        ;; test suite. MK.
        payload    ""
        queue      "langohr.examples.publishing.using-default-exchange"
        declare-ok (lhq/declare channel queue { :auto-delete true })
        tag        (lhu/generate-consumer-tag "langohr.basic/consume-tests")

        content-type "text/plain"
        msg-id       (.toString (java.util.UUID/randomUUID))
        n            3000
        latch        (java.util.concurrent.CountDownLatch. n)
        msg-handler   (fn [delivery message-properties message-payload]
                        (print ".")
                        (.countDown latch))]
    (.start (Thread. #((lhb/consume channel queue msg-handler { :consumer-tag tag, :auto-ack true })) "consumer"))
    (.start (Thread. (fn []
                       (dotimes [i n]
                         (lhb/publish channel exchange queue payload :priority 8, :message-id msg-id, :content-type content-type, :headers { "see you soon" "à bientôt" }))) "publisher"))
    (.await latch)))






;;
;; basic.get
;;

(deftest t-basic-get-with-automatic-ack
  (let [channel    (.createChannel *conn*)
        exchange   ""
        payload    "A message we will fetch with basic.get"
        queue      "langohr.examples.basic.get.queue1"
        declare-ok (lhq/declare channel queue { :auto-delete true })]
    (lhb/publish channel exchange queue payload)
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
        queue      "langohr.examples.basic.get.queue2"
        declare-ok (lhq/declare channel queue { :auto-delete true })]
    (lhb/publish channel exchange queue payload)
    (let [get-response (lhb/get channel queue false)]
      (is (instance? GetResponse get-response))
      (is (= (String. (.getBody get-response)) payload)))))


(deftest t-basic-get-with-an-empty-queue
  (let [channel    (.createChannel *conn*)
        queue      (.getQueue (lhq/declare channel "" { :auto-delete true }))]
    (is (nil? (lhb/get channel queue false)))))



;;
;; basic.qos
;;

(deftest t-using-non-global-basic-qos
  (let [channel (.createChannel *conn*)]
    (lhb/qos channel 5)))


;;
;; basic.ack
;;

(deftest t-acknowledge-one-message
  (let [producer-channel (.createChannel *conn*)
        consumer-channel (.createChannel *conn*)
        queue            (.getQueue (lhq/declare consumer-channel "langohr.examples.basic.ack.queue1" { :auto-delete true }))]
    (lhq/purge   producer-channel queue)
    (.start (Thread. ^Callable (fn []
                                 (lhb/publish producer-channel "" queue "One")
                                 (lhb/publish producer-channel "" queue "Two")
                                 (lhb/publish producer-channel "" queue "Three"))))
    (Thread/sleep 200)
    (let [get-response (lhb/get consumer-channel queue false)
          delivery-tag (.. get-response getEnvelope getDeliveryTag)]
      (is (= 1 delivery-tag))
      (lhb/ack consumer-channel delivery-tag))
    (lhq/purge   producer-channel queue)))

(deftest t-acknowledge-multiple-messages
  (let [producer-channel (.createChannel *conn*)
        consumer-channel (.createChannel *conn*)
        queue            (.getQueue (lhq/declare consumer-channel "langohr.examples.basic.ack.queue2" { :auto-delete true }))]
    (lhq/purge   producer-channel queue)
    (.start (Thread. ^Callable (fn []
                                 (lhb/publish producer-channel "" queue "One")
                                 (lhb/publish producer-channel "" queue "Two")
                                 (lhb/publish producer-channel "" queue "Three"))))
    (Thread/sleep 200)
    (let [get-response1 (lhb/get consumer-channel queue false)
          get-response2 (lhb/get consumer-channel queue false)
          delivery-tag1  (.. get-response1 getEnvelope getDeliveryTag)
          delivery-tag2  (.. get-response2 getEnvelope getDeliveryTag)]
      (is (= 1 delivery-tag1))
      (is (= 2 delivery-tag2))
      (lhb/ack consumer-channel delivery-tag1 true))
    (lhq/purge   producer-channel queue)))


;;
;; basic.nack
;;

(deftest t-nack-one-message-to-requeue-it
  (let [channel (.createChannel *conn*)
        queue   (.getQueue (lhq/declare channel "langohr.examples.basic.nack.queue1" { :auto-delete true }))]
    (lhq/purge channel queue)
    (.start (Thread. ^Callable (fn []
                                 (lhb/publish channel "" queue "One")
                                 (lhb/publish channel "" queue "Two")
                                 (lhb/publish channel "" queue "Three"))))
    (Thread/sleep 200)
    (let [get-response (lhb/get channel queue false)
          delivery-tag (.. get-response getEnvelope getDeliveryTag)]
      (is (= 1 delivery-tag))
      (lhb/nack channel delivery-tag false true))
    (lhq/purge channel queue)))

(deftest t-nack-multiple-messages-without-requeueing
  (let [channel (.createChannel *conn*)
        queue   (.getQueue (lhq/declare channel "langohr.examples.basic.nack.queue2" { :auto-delete true }))]
    (lhq/purge channel queue)
    (.start (Thread. ^Callable (fn []
                                 (lhb/publish channel "" queue "One")
                                 (lhb/publish channel "" queue "Two")
                                 (lhb/publish channel "" queue "Three"))))
    (Thread/sleep 200)
    (let [get-response1 (lhb/get channel queue false)
          get-response2 (lhb/get channel queue false)
          delivery-tag1  (.. get-response1 getEnvelope getDeliveryTag)
          delivery-tag2  (.. get-response2 getEnvelope getDeliveryTag)]
      (is (= 1 delivery-tag1))
      (is (= 2 delivery-tag2))
      (lhb/nack channel delivery-tag1 true false))
    (lhq/purge channel queue)))



;;
;; basic.reject
;;

(deftest t-reject-one-message-to-requeue-it
  (let [channel (.createChannel *conn*)
        queue   (.getQueue (lhq/declare channel "langohr.examples.basic.reject.queue1" { :auto-delete true }))]
    (lhq/purge channel queue)
    (.start (Thread. ^Callable (fn []
                                 (lhb/publish channel "" queue "One")
                                 (lhb/publish channel "" queue "Two")
                                 (lhb/publish channel "" queue "Three"))))
    (Thread/sleep 200)
    (let [get-response (lhb/get channel queue false)
          delivery-tag (.. get-response getEnvelope getDeliveryTag)]
      (is (= 1 delivery-tag))
      (lhb/reject channel delivery-tag true))
    (lhq/purge channel queue)))

(deftest t-reject-one-message-without-requeueing
  (let [channel (.createChannel *conn*)
        queue   (.getQueue (lhq/declare channel "langohr.examples.basic.reject.queue2" { :auto-delete true }))]
    (lhq/purge channel queue)
    (.start (Thread. ^Callable (fn []
                                 (lhb/publish channel "" queue "One")
                                 (lhb/publish channel "" queue "Two")
                                 (lhb/publish channel "" queue "Three"))))
    (Thread/sleep 200)
    (let [get-response1 (lhb/get channel queue false)
          get-response2 (lhb/get channel queue false)
          delivery-tag1  (.. get-response1 getEnvelope getDeliveryTag)
          delivery-tag2  (.. get-response2 getEnvelope getDeliveryTag)]
      (is (= 1 delivery-tag1))
      (is (= 2 delivery-tag2))
      (lhb/reject channel delivery-tag1 false))
    (lhq/purge channel queue)))
