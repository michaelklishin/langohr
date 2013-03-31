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

(deftest t-connection-with-uri
  (let [conn (connect {:uri "amqp://127.0.0.1:5672"})]
    (is (open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))
    (is (-> conn .getServerProperties (get "capabilities") (get "publisher_confirms")))))

(deftest t-connection-to-first-available-with-default-parameters
  ;; see ./bin/ci/before_script.sh
  (let [conn (connect-to-first-available
              [["127.0.0.1" 0]
               ["127.0.0.1" 5672]])]
    (is (open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))))

(deftest t-connection-to-first-available-missing-port
  ;; see ./bin/ci/before_script.sh
  (let [conn (connect-to-first-available
              [["127.0.0.1"]])]
    (is (open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))))

(deftest t-connection-to-first-available-with-overriden-parameters
  ;; see ./bin/ci/before_script.sh
  (let [conn (connect-to-first-available
              [["127.0.0.1" 0]
               ["127.0.0.1" 5672]]
              {:vhost "langohr_testbed"
               :username "langohr" :password "langohr.password"
               :requested-heartbeat 3 :connection-timeout 5 })]
    (is (open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))
    (is (= 3           (.getHeartbeat conn)))))

(deftest t-broker-capabilities
  (let [conn (connect {:uri "amqp://127.0.0.1:5672"})]
    (is (= {:exchange_exchange_bindings true
            :consumer_cancel_notify true
            :basic.nack true
            :publisher_confirms true}
           (capabilities-of conn)))))


(deftest t-connection-failure-due-to-misconfigured-port
  (is (thrown? java.net.ConnectException (connect { :host "127.0.0.1" :port 2887 }))))

(deftest t-connection-failure-due-to-unknown-host
  (is (thrown? java.net.UnknownHostException (connect { :host "skdjhfkjshfglkashfklajshdf.local" :port 2887 }))))

(deftest t-connection-failure-due-to-invalid-credentials
  (is (thrown? com.rabbitmq.client.PossibleAuthenticationFailureException (connect { :username "skdjhfkjshFGLKASHFKlajshdf" :password "HFKlajshdf" }))))


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
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "product" :password "guest"} m))))
  (testing "case where path contains dots"
    (let [uri "amqp://dev.rabbitmq.com/a.b.c"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "a.b.c" :password "guest"} m))))
  (testing "case where path uses URL encoding"
    (let [uri "amqp://dev.rabbitmq.com/%2Fvault"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :vhost "/vault" :username "guest" :password "guest"} m))))
  (testing "case where path uses URL encoding"
    (let [uri "amqp://dev.rabbitmq.com/foo%2Fbar"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :vhost "foo/bar" :username "guest" :password "guest"} m))))
  (testing "with a sample CloudFoundry URI"
    (let [uri "amqp://utquQluArWWn3:vZ19hISpc3ICU@172.22.87.188:87888/5e56ec8f588b44f17213b6d756v544a70"
          m   (settings-from uri)]
      (is (= {:host "172.22.87.188" :port 87888 :vhost "5e56ec8f588b44f17213b6d756v544a70" :username "utquQluArWWn3" :password "vZ19hISpc3ICU"} m)))))

(deftest t-uri-parsing-for-amqps
  (testing "case without the path part"
    (let [uri "amqps://dev.rabbitmq.com"
          m   (settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5671 :username "guest" :vhost "/" :password "guest"} m)))))
