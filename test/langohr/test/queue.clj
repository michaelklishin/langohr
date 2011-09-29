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

(defonce ^Connection conn (lhc/connect))


(deftest t-declare-a-server-named-queue-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        declare-ok (lhq/declare channel)]
    (is (re-seq #"^amq\.gen-(.+)" (.getQueue declare-ok)))
    (is (= 0 (.getConsumerCount declare-ok)))
    (is (= 0 (.getMessageCount declare-ok)))))


(deftest t-declare-a-client-named-queue-with-default-attributes
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)]
    (is (= (.getQueue declare-ok) queue-name))))


(deftest t-declare-a-non-durable-exclusive-auto-deleted-client-named-queue
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named.non-durable.exclusive.auto-deleted"
         declare-ok (lhq/declare channel queue-name :durable false, :exclusive true, :auto-delete true)]
    (is (= (.getQueue declare-ok) queue-name))))


(deftest t-declare-a-durable-non-exclusive-non-auto-deleted-client-named-queue
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named.durable.non-exclusive.non-auto-deleted"
         declare-ok (lhq/declare channel queue-name :durable true, :exclusive false, :auto-delete false)]
    (is (= (.getQueue declare-ok) queue-name))))


;;
;; Passive queue.declare
;;

(deftest t-passive-declare-a-non-durable-exclusive-auto-deleted-client-named-queue
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named.non-durable.exclusive.auto-deleted"
         _          (lhq/declare channel queue-name :durable false, :exclusive true, :auto-delete true)
         status     (lhq/status channel queue-name)]
    (is (= 0 (status :message-count)))
    (is (= 0 (status :consumer-count)))))


;;
;; queue.bind
;;

(deftest t-bind-a-server-named-queue-to-amq-fanout
  (let [channel  (lhc/create-channel conn)
        declare-ok (lhq/declare channel)
        queue    (.getQueue declare-ok)
        exchange "amq.fanout"]
    (lhq/bind channel queue exchange)))


(deftest t-bind-a-client-named-queue-to-amq-fanout
  (let [channel  (lhc/create-channel conn)
        queue    "langohr.tests.queues.client-named.non-durable.exclusive.auto-deleted"
        exchange "amq.fanout"]
    (lhq/declare channel queue)
    (lhq/bind    channel queue exchange)))


;;
;; queue.unbind
;;

(deftest t-unbind-a-server-named-queue-from-amq-fanout
  (let [channel  (lhc/create-channel conn)
        declare-ok (lhq/declare channel)
        queue    (.getQueue declare-ok)
        exchange "amq.fanout"]
    (lhq/bind   channel queue exchange :routing-key "abc")
    (lhq/unbind channel queue exchange "abc")))



;;
;; queue.delete
;;

(deftest t-declare-and-immediately-delete-a-client-named-queue-with-default-attributes
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)]
    (lhq/delete channel queue-name)))


(deftest t-declare-and-immediately-delete-a-client-named-queue-if-it-is-empty
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)]
    (lhq/delete channel queue-name false true)))


(deftest t-declare-and-immediately-delete-a-client-named-queue-if-it-is-unused
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)]
    (lhq/delete channel queue-name true false)))


;;
;; queue.purge
;;

(deftest t-purge-a-queue-without-messages
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)]
    (is (= (.getMessageCount declare-ok) 0))
    (lhq/purge channel queue-name)
    (is (= (.getMessageCount declare-ok) 0))))

(deftest t-purge-a-queue-that-has-messages-in-it
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)
         n                                30]
    (doseq [i (range n)]
      (lhb/publish channel "" queue-name "Hi"))
    (Thread/sleep 200)
    (is (= (.getMessageCount (lhq/declare channel queue-name)) n))
    (lhq/purge channel queue-name)
    (is (= (.getMessageCount (lhq/declare channel queue-name)) 0))))
