(set! *warn-on-reflection* true)

(ns langohr.test.queue
  (:refer-clojure :exclude [declare get])
  (:import (com.rabbitmq.client Connection Channel AMQP AMQP$Queue$DeclareOk AMQP$Queue$BindOk AMQP$Queue$UnbindOk))
  (:use [clojure.test]
        [langohr.core  :as lhc]
        [langohr.queue :as lhq]
        [langohr.basic :as lhb]))

;;
;; queue.declare
;;

(defonce ^:dynamic ^Connection *conn* (lhc/connect))


(deftest t-declare-a-server-named-queue-with-default-attributes
  (let [^Channel              channel    (lhc/create-channel *conn*)
        ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel)]
    (is (lhc/open? *conn*))
    (is (lhc/open? channel))
    (is (instance? AMQP$Queue$DeclareOk declare-ok))
    (is (re-seq #"^amq\.gen-(.+)" (.getQueue declare-ok)))
    (is (= 0 (.getConsumerCount declare-ok)))
    (is (= 0 (.getMessageCount declare-ok)))))


(deftest t-declare-a-client-named-queue-with-default-attributes
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named-with-default-attributes"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name)]
    (is (= (.getQueue declare-ok) queue-name))))


(deftest t-declare-a-non-durable-exclusive-auto-deleted-client-named-queue
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named.non-durable.exclusive.auto-deleted"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name { :durable false, :exclusive true, :auto-delete true })]
    (is (= (.getQueue declare-ok) queue-name))))


(deftest t-declare-a-durable-non-exclusive-non-auto-deleted-client-named-queue
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named.durable.non-exclusive.non-auto-deleted"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name { :durable true, :exclusive false, :auto-delete false })]
    (is (= (.getQueue declare-ok) queue-name))))



;;
;; queue.bind
;;

(deftest t-bind-a-server-named-queue-to-amq-fanout
  (let [^Channel              channel  (lhc/create-channel *conn*)
        ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel)
        queue    (.getQueue declare-ok)
        exchange "amq.fanout"
        bind-ok  (lhq/bind channel queue exchange)]
    (is (instance? AMQP$Queue$BindOk bind-ok))))


(deftest t-bind-a-client-named-queue-to-amq-fanout
  (let [channel  (lhc/create-channel *conn*)
        queue    "langohr.tests.queues.client-named.non-durable.exclusive.auto-deleted"
        exchange "amq.fanout"
        bind-ok  (do
                   (lhq/declare channel queue)
                   (lhq/bind    channel queue exchange))]
        (is (instance? AMQP$Queue$BindOk bind-ok))))


;;
;; queue.unbind
;;

(deftest t-unbind-a-server-named-queue-from-amq-fanout
  (let [^Channel              channel  (lhc/create-channel *conn*)
        ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel)
        queue    (.getQueue declare-ok)
        exchange "amq.fanout"
        bind-ok    (lhq/bind   channel queue exchange :routing-key "abc")
        unbind-ok  (lhq/unbind channel queue exchange "abc")]
    (is (instance? AMQP$Queue$UnbindOk unbind-ok))))



;;
;; queue.delete
;;

(deftest t-declare-and-immediately-delete-a-client-named-queue-with-default-attributes
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named-with-default-attributes"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name)]
    (lhq/delete channel queue-name)))


(deftest t-declare-and-immediately-delete-a-client-named-queue-if-it-is-empty
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named-with-default-attributes"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name)]
    (lhq/delete channel queue-name false true)))


(deftest t-declare-and-immediately-delete-a-client-named-queue-if-it-is-unused
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named-with-default-attributes"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name)]
    (lhq/delete channel queue-name true false)))


;;
;; queue.purge
;;

(deftest t-purge-a-queue-without-messages
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named-with-default-attributes"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name)]
    (is (= (.getMessageCount declare-ok) 0))
    (lhq/purge channel queue-name)
    (is (= (.getMessageCount declare-ok) 0))))

(deftest t-purge-a-queue-that-has-messages-in-it
  (let  [^Channel              channel    (lhc/create-channel *conn*)
         ^String               queue-name "langohr.tests.queues.client-named-with-default-attributes"
         ^AMQP$Queue$DeclareOk declare-ok (lhq/declare channel queue-name)
         n                                30]
    (doseq [i (range n)]
      (lhb/publish channel "" queue-name "Hi"))
    (Thread/sleep 200)
    (is (= (.getMessageCount (lhq/declare channel queue-name)) n))
    (lhq/purge channel queue-name)
    (is (= (.getMessageCount (lhq/declare channel queue-name)) 0))))
