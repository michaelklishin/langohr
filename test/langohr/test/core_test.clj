(ns langohr.test.core-test
  (:require [langohr.core     :as lc]
            [langohr.shutdown :as ls])
  (:use clojure.test)
  (:import java.util.concurrent.Executors))

(set! *warn-on-reflection* true)
(println (str "Using Clojure version " *clojure-version*))


(deftest t-connection-with-default-parameters
  (let [conn (lc/connect)]
    (is (instance? com.rabbitmq.client.Connection conn))
    (is (not (lc/automatically-recover? conn)))
    (is (lc/open? conn))))

(deftest t-connection-with-overriden-parameters
  ;; see ./bin/ci/before_script.sh
  (let [conn (lc/connect {
                       :host "127.0.0.1" :port 5672
                       :vhost "langohr_testbed" :username "langohr" :password "langohr.password"
                       :requested-heartbeat 3 :connection-timeout 5 })]
    (is (lc/open? conn))
    (is (not (lc/automatically-recover? conn)))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))
    (is (= 3           (.getHeartbeat conn)))))

(deftest t-connection-with-custom-executor
  (let [conn (lc/connect {:executor (Executors/newFixedThreadPool 16)})]
    (is (lc/open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))))

(deftest t-connection-with-uri
  (let [conn (lc/connect {:uri "amqp://127.0.0.1:5672"})]
    (is (lc/open? conn))
    (is (= "127.0.0.1" (-> conn .getAddress .getHostAddress)))
    (is (= 5672        (.getPort conn)))
    (is (-> conn .getServerProperties (get "capabilities") (get "publisher_confirms")))))

(deftest t-connection-with-connection-recovery-enabled
  (let [conn (lc/connect {:automatically-recover true})]
    (is (lc/automatically-recover? conn))
    (is (lc/open? conn))))

(deftest t-broker-capabilities
  (let [conn (lc/connect {:uri "amqp://127.0.0.1:5672"})
        m    (lc/capabilities-of conn)]
    (is (:exchange_exchange_bindings m))
    (is (:consumer_cancel_notify m))
    (is (:basic.nack m))
    (is (:publisher_confirms m))))


(deftest t-connection-failure-due-to-misconfigured-port
  (is (thrown? java.net.ConnectException (lc/connect { :host "127.0.0.1" :port 2887 }))))

(deftest t-connection-failure-due-to-unknown-host
  (is (thrown? java.net.UnknownHostException (lc/connect { :host "skdjhfkjshfglkashfklajshdf.local" :port 2887 }))))

(deftest t-connection-failure-due-to-invalid-credentials
  (is (thrown? com.rabbitmq.client.PossibleAuthenticationFailureException (lc/connect { :username "skdjhfkjshFGLKASHFKlajshdf" :password "HFKlajshdf" }))))


(deftest t-close-connection
  (let [conn (lc/connect)]
    (is (lc/open? conn))
    (lc/close conn)
    (is (not (lc/open? conn)))))

(deftest t-uri-parsing-for-amqp
  (testing "case without the path part"
    (let [uri "amqp://dev.rabbitmq.com"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "/" :password "guest"} m))))
  (testing "case where path is a single slash"
    (let [uri "amqp://dev.rabbitmq.com/"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "" :password "guest"} m))))
  (testing "case where path equals /product"
    (let [uri "amqp://dev.rabbitmq.com/product"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "product" :password "guest"} m))))
  (testing "case where path contains dots"
    (let [uri "amqp://dev.rabbitmq.com/a.b.c"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :username "guest" :vhost "a.b.c" :password "guest"} m))))
  (testing "case where path uses URL encoding"
    (let [uri "amqp://dev.rabbitmq.com/%2Fvault"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :vhost "/vault" :username "guest" :password "guest"} m))))
  (testing "case where path uses URL encoding"
    (let [uri "amqp://dev.rabbitmq.com/foo%2Fbar"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5672 :vhost "foo/bar" :username "guest" :password "guest"} m))))
  (testing "with a sample CloudFoundry URI"
    (let [uri "amqp://utquQluArWWn3:vZ19hISpc3ICU@172.22.87.188:87888/5e56ec8f588b44f17213b6d756v544a70"
          m   (lc/settings-from uri)]
      (is (= {:host "172.22.87.188" :port 87888 :vhost "5e56ec8f588b44f17213b6d756v544a70" :username "utquQluArWWn3" :password "vZ19hISpc3ICU"} m)))))

(deftest t-uri-parsing-for-amqps
  (testing "case without the path part"
    (let [uri "amqps://dev.rabbitmq.com"
          m   (lc/settings-from uri)]
      (is (= {:host "dev.rabbitmq.com" :port 5671 :username "guest" :vhost "/" :password "guest"} m)))))

(deftest t-connection-after-recovery-is-wrapper
  (let [conn  (lc/connect {:automatically-recover true})
        conn-delegate (cast com.rabbitmq.client.impl.AMQConnection (.getDelegate conn))
        ch    (lc/create-channel conn)
        latch (java.util.concurrent.CountDownLatch. 3)]
    (is (lc/automatically-recover? conn))
    (lc/on-recovery conn (fn [new-conn] 
                           (.countDown latch)
                           (is (instance? com.novemberain.langohr.Connection new-conn)))) 
    (lc/on-recovery ch (fn [new-channel] 
                           (.countDown latch)
                           (is (instance? com.novemberain.langohr.Channel new-channel))))
    (.addShutdownListener conn 
      (lc/shutdown-listener 
        (fn [sse] 
          (.countDown latch)
          (is (not (ls/initiated-by-application? sse))))))
    (.close conn-delegate 200 "simulated broken connection" false (RuntimeException.))
    (is (.await latch 30 java.util.concurrent.TimeUnit/SECONDS) "timeout waiting for recovery hooks")))