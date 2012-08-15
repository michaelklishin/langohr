(ns langohr.test.core-test
  (:import [com.rabbitmq.client Connection Channel])
  (:use clojure.test langohr.core))

(set! *warn-on-reflection* true)
(println (str "Using Clojure version " *clojure-version*))


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

(deftest t-uri-parsing-for-amqp
  (testing "case without the path part"
    (let [uri "amqp://dev.rabbitmq.com"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "/" :password "guest"} m))))
  (testing "case where path is a single slash"
    (let [uri "amqp://dev.rabbitmq.com/"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "" :password "guest"} m))))
  (testing "case where path equals /product"
    (let [uri "amqp://dev.rabbitmq.com/product"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "product" :password "guest"} m)))))

(deftest t-uri-parsing-for-amqps
  (testing "case without the path part"
    (let [uri "amqps://dev.rabbitmq.com"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5671 :username "guest" :vhost "/" :password "guest"} m)))))
