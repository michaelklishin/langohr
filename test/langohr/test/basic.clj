(set! *warn-on-reflection* true)

(ns langohr.test.basic
  (:import (com.rabbitmq.client Connection Channel AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder QueueingConsumer))
  (:use [clojure.test] [langohr.core :as lhc] [langohr.queue :as lhq] [langohr.basic :as lhb]))

;;
;; basic.publish, basic.consume
;;

(defonce ^Connection *conn* (lhc/connect))


(deftest t-publishing-using-default-exchange-and-default-message-attributes
  (let [channel    (.createChannel *conn*)
        exchange   ""
        ;; yes, payload may be blank. This is an edge case Ruby amqp
        ;; gem did not support for a long time so I want to use it in the langohr
        ;; test suite. MK.
        payload    ""
        queue      "langohr.examples.publishing.using-default-exchange"
        declare-ok (lhq/declare channel queue { :auto-delete true })]
    (lhb/publish channel payload { :routing-key queue, :exchange exchange })))

(deftest t-using-non-global-basic-qos
  (let [channel (.createChannel *conn*)]
    (lhb/qos channel 5)))

