(set! *warn-on-reflection* true)
(println (str "Using Clojure version " *clojure-version*))

(ns langohr.test.core
  (:import (com.rabbitmq.client Connection Channel))
  (:use [clojure.test] [langohr.core]))

(deftest t-connection-with-default-parameters
  (let [conn (connect)]
    (is (instance? com.rabbitmq.client.Connection conn))
    (is (open? conn))))

(deftest t-close-connection
  (let [conn (connect)]
    (is (open? conn))
    (close conn)
    (is (not (open? conn)))))
