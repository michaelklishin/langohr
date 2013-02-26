(ns langohr.test.queue-test
  (:refer-clojure :exclude [declare get])
  (:require [langohr.core  :as lhc]
            [langohr.queue :as lhq]
            [langohr.basic :as lhb])
  (:use clojure.test)
  (:import [com.rabbitmq.client Connection Channel AMQP AMQP$Queue$DeclareOk AMQP$Queue$BindOk AMQP$Queue$UnbindOk]
           java.io.IOException))

;;
;; queue.declare
;;

(defonce ^Connection conn (lhc/connect))


(deftest test-declare-a-server-named-queue-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        {:keys [consumer-count message-count queue]} (lhq/declare channel)]
    (is (re-seq #"^amq\.gen-(.+)" queue))
    (is (= 0 consumer-count))
    (is (= 0 message-count))))


(deftest test-declare-a-client-named-queue-with-default-attributes
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests2.queues.client-named-with-default-attributes"
         {:keys [queue]} (lhq/declare channel queue-name)]
    (is (= queue queue-name))))


(deftest test-declare-a-durable-non-exclusive-non-auto-deleted-client-named-queue
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests2.queues.client-named.durable.non-exclusive.non-auto-deleted"
         {:keys [queue]} (lhq/declare channel queue-name :durable true :exclusive false :auto-delete false)]
    (is (= queue queue-name))))


(deftest test-redeclare-an-auto-deleted-queue-with-different-attributes
  (let [conn        (lhc/connect)
        channel     (lhc/create-channel conn)
        queue       "langohr.tests2.queues.non-auto-deleted1"
        shutdown-ln (lhc/shutdown-listener (fn [cause]
                                             (println "Shutdown listener has fired")))]
    (try
      (.addShutdownListener channel shutdown-ln)
      (lhq/declare channel queue :auto-delete true)
      (lhq/declare channel queue :auto-delete false)
      (catch IOException ioe ;; see http://www.rabbitmq.com/api-guide.html#shutdown
        nil))))


(deftest test-queue-declaration-with-message-ttl
  (let [channel  (lhc/create-channel conn)
        {:keys [queue]} (lhq/declare channel "" :auto-delete true :arguments { "x-message-ttl" 1500 } )]
    (lhb/publish channel "" queue "")
    (Thread/sleep 1700)
    (is (nil? (lhb/get channel queue)))))


(deftest test-queue-declaration-with-queue-ttl
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

(deftest test-passive-declare-a-non-durable-exclusive-auto-deleted-client-named-queue
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests2.queues.client-named.non-durable.non-exclusive.auto-deleted"
         _          (lhq/declare channel queue-name :durable false :exclusive false :auto-delete true)
         status     (lhq/status channel queue-name)]
    (is (= 0 (status :message-count)))
    (is (= 0 (status :consumer-count)))))


;;
;; queue.bind
;;

(deftest test-bind-a-server-named-queue-to-amq-fanout
  (let [channel  (lhc/create-channel conn)
        {:keys [queue]} (lhq/declare channel)
        exchange "amq.fanout"]
    (lhq/bind channel queue exchange)))


(deftest test-bind-a-client-named-queue-to-amq-fanout
  (let [channel  (lhc/create-channel conn)
        queue    "langohr.tests2.queues.client-named.non-durable.non-exclusive.auto-deleted"
        exchange "amq.fanout"]
    (lhq/declare channel queue)
    (lhq/bind    channel queue exchange)))


;;
;; queue.unbind
;;

(deftest test-unbind-a-server-named-queue-from-amq-fanout
  (let [channel  (lhc/create-channel conn)
        {:keys [queue]} (lhq/declare channel)
        exchange "amq.fanout"]
    (lhq/bind   channel queue exchange :routing-key "abc")
    (lhq/unbind channel queue exchange "abc")))



;;
;; queue.delete
;;

(deftest test-declare-and-immediately-delete-a-client-named-queue-with-default-attributes
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests2.queues.client-named-with-default-attributes"]
    (lhq/declare         channel queue-name)
    (lhq/declare-passive channel queue-name)
    (lhq/delete          channel queue-name)
    (is (thrown? java.io.IOException
                 (lhq/declare-passive channel queue-name)))))


(deftest test-declare-and-immediately-delete-a-client-named-queue-if-it-is-empty
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests2.queues.client-named-with-default-attributes"]
    (lhq/declare channel queue-name)
    (lhq/delete channel queue-name false true)))


(deftest test-declare-and-immediately-delete-a-client-named-queue-if-it-is-unused
  (let  [channel    (lhc/create-channel conn)
         queue-name "langohr.tests2.queues.client-named-with-default-attributes"]
    (lhq/declare channel queue-name)
    (lhq/delete channel queue-name true false)))


;;
;; queue.purge
;;

(deftest test-purge-a-queue-without-messages
  (let  [channel (lhc/create-channel conn)
         queue   "langohr.tests2.queues.client-named-with-default-attributes"]
    (lhq/declare channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))
    (lhq/purge channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))))

(deftest test-purge-a-queue-that-has-messages-in-it
  (let  [channel (lhc/create-channel conn)
         queue   "langohr.tests2.queues.client-named-with-default-attributes"
         n       30]
    (lhq/declare channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))
    (doseq [i (range n)]
      (lhb/publish channel "" queue "Hi"))
    (Thread/sleep 200)
    (is (= n (:message-count (lhq/status channel queue))))
    (lhq/purge channel queue)
    (is (= 0 (:message-count (lhq/status channel queue))))))
