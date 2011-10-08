(set! *warn-on-reflection* true)

(ns langohr.test.queue
  (:refer-clojure :exclude [declare get])
  (:import (com.rabbitmq.client Connection Channel AMQP AMQP$Queue$DeclareOk AMQP$Queue$BindOk AMQP$Queue$UnbindOk)
           (java.io IOException))
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


(deftest t-redeclare-an-auto-deleted-queue-with-different-attributes
  (let [conn        (lhc/connect)
        channel     (lhc/create-channel conn)
        queue       "langohr.tests.queues.non-auto-deleted1"
        shutdown-ln (lhc/shutdown-listener (fn [cause]
                                             (println "Shutdown listener has fired")))]
    (try
      (.addShutdownListener channel shutdown-ln)
      (lhq/declare channel queue :auto-delete true)
      (lhq/declare channel queue :auto-delete false)
      (catch IOException ioe ;; see http://www.rabbitmq.com/api-guide.html#shutdown
        nil))))


(deftest t-queue-declaration-with-message-ttl
  (let [channel  (lhc/create-channel conn)
        queue    (.getQueue (lhq/declare channel "" :auto-delete true :arguments { "x-message-ttl" 1500 } ))]
    (lhb/publish channel "" queue "")
    (Thread/sleep 1700)
    (is (nil? (lhb/get channel queue)))))


(deftest t-queue-declaration-with-queue-ttl
  (let [channel  (lhc/create-channel conn)
        queue    "langohr.test.leased.queue"]
    (lhq/declare channel queue :auto-delete true :exclusive false :arguments { "x-expires" 1500 } )
    (lhq/declare-passive channel queue)
    (lhb/publish channel "" queue "")
    (Thread/sleep 1700)
    (try
      (lhq/declare-passive channel queue)
      (catch IOException ioe
        nil))))



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
         queue-name "langohr.tests.queues.client-named-with-default-attributes"]
    (lhq/declare         channel queue-name)
    (lhq/declare-passive channel queue-name)
    (lhq/delete          channel queue-name)
    (is (thrown? java.io.IOException
                 (lhq/declare-passive channel queue-name)))))


(deftest t-declare-and-immediately-delete-a-client-named-queue-if-it-is-empty
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"]
    (lhq/declare channel queue-name)
    (lhq/delete channel queue-name false true)))


(deftest t-declare-and-immediately-delete-a-client-named-queue-if-it-is-unused
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"]
    (lhq/declare channel queue-name)
    (lhq/delete channel queue-name true false)))


;;
;; queue.purge
;;

(deftest t-purge-a-queue-without-messages
  (let  [channel (lhc/create-channel conn)
         queue   "langohr.tests.queues.client-named-with-default-attributes"]
    (lhq/declare channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))
    (lhq/purge channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))))

(deftest t-purge-a-queue-that-has-messages-in-it
  (let  [channel (lhc/create-channel conn)
         queue   "langohr.tests.queues.client-named-with-default-attributes"
         n       30]
    (lhq/declare channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))
    (doseq [i (range n)]
      (lhb/publish channel "" queue "Hi"))
    (Thread/sleep 200)
    (is (= n (:message-count (lhq/status channel queue))))
    (lhq/purge channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))))
