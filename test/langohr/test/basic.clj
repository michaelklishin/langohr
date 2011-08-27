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
                         (lhb/publish channel exchange queue payload { :priority 8, :message-id msg-id, :content-type content-type, :headers { "see you soon" "à bientôt" } }))) "publisher"))
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
    (lhb/publish channel exchange queue payload {})
    (let [get-response (lhb/get channel queue)]
      (is (instance? GetResponse get-response))
      (is (= (String. (.getBody get-response)) payload))
      (is (= (.getMessageCount get-response) 0))
      (is (= (.. get-response getEnvelope getExchange) exchange))
      (is (= (.. get-response getEnvelope getRoutingKey) queue)))))

(deftest t-basic-get-with-automatic-ack
  (let [channel    (.createChannel *conn*)
        exchange   ""
        payload    "A message we will fetch with basic.get"
        queue      "langohr.examples.basic.get.queue1"
        declare-ok (lhq/declare channel queue { :auto-delete true })]
    (lhb/publish channel exchange queue payload {})
    (let [get-response (lhb/get channel queue false)]
      (is (instance? GetResponse get-response))
      (is (= (String. (.getBody get-response)) payload)))))





;;
;; basic.qos
;;

(deftest t-using-non-global-basic-qos
  (let [channel (.createChannel *conn*)]
    (lhb/qos channel 5)))

