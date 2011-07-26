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
