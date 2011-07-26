(ns langohr.test.queue
  (:import (com.rabbitmq.client Channel AMQP AMQP$Queue$DeclareOk))
  (:use [clojure.test] [langohr.core :as lhc] [langohr.queue :as lhq]))

(deftest t-declare-a-server-named-queue-with-default-attributes
  (let [conn       (lhc/connect)
        ch         (lhc/create-channel conn)
        declare-ok (lhq/declare ch)]
    (is (lhc/open? conn))
    (is (lhc/open? ch))
    (is (instance? AMQP$Queue$DeclareOk declare-ok))
    (is (re-seq #"^amq\.gen-(.+)" (.getQueue declare-ok)))
    (is (= 0 (.getConsumerCount declare-ok)))
    (is (= 0 (.getMessageCount declare-ok)))))


(deftest t-declare-a-client-named-queue-with-default-attributes
  (let  [conn       (lhc/connect)
         channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named-with-default-attributes"
         declare-ok (lhq/declare channel queue-name)]
    (is (= (.getQueue declare-ok) queue-name))))


(deftest t-declare-a-non-durable-exclusive-auto-deleted-client-named-queue
  (let  [conn       (lhc/connect)
         channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named.non-durable.exclusive.auto-deleted"
         declare-ok (lhq/declare channel queue-name { :durable false, :exclusive true, :auto-delete true })]
    (is (= (.getQueue declare-ok) queue-name))))


(deftest t-declare-a-durable-non-exclusive-non-auto-deleted-client-named-queue
  (let  [conn       (lhc/connect)
         channel    (lhc/create-channel conn)
         queue-name "langohr.tests.queues.client-named.durable.non-exclusive.non-auto-deleted"
         declare-ok (lhq/declare channel queue-name { :durable true, :exclusive false, :auto-delete false })]
    (is (= (.getQueue declare-ok) queue-name))))

