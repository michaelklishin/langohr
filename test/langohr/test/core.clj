(set! *warn-on-reflection* true)
(println (str "Using Clojure version " *clojure-version*))

(ns langohr.test.core
  (:import (com.rabbitmq.client Connection Channel))
  (:use [clojure.test] [langohr.core]))

(deftest t-connection-with-default-parameters
  (let [conn (connect)]
    (is (instance? com.rabbitmq.client.Connection conn))
    (is (open? conn))))

(deftest t-connection-with-overriden-parameters
  ;; see ./bin/ci/before_script.sh
  (let [conn (connect {
                       :host "127.0.0.1" :port 5672
                       :vhost "langohr_testbed" :username "langohr" :password "langohr.password"
                       :requested-heartbeat 3 :connection-timeout 5 })]
    (is (open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))
    (is (= 3           (.getHeartbeat conn)))))


(deftest t-connection-failure-due-to-misconfigured-port
  (is (thrown? java.net.ConnectException (connect { :host "127.0.0.1" :port 2887 }))))

(deftest t-connection-failure-due-to-unknown-host
  (is (thrown? java.net.UnknownHostException (connect { :host "skdjhfkjshfglkashfklajshdf.local" :port 2887 }))))


(deftest t-close-connection
  (let [conn (connect)]
    (is (open? conn))
    (close conn)
    (is (not (open? conn)))))
