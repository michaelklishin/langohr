;; Copyright (c) 2011-2025 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.exchange-test
  (:refer-clojure :exclude [declare])
  (:require [langohr.core     :as lhc]
            [langohr.exchange :as lhe]
            [langohr.queue    :as lhq]
            [langohr.consumers :as lhcons]
            [langohr.basic    :as lhb]
            [clojure.test     :refer :all])
  (:import [com.rabbitmq.client Connection Channel
            AMQP AMQP$Exchange$DeclareOk AMQP$Exchange$DeleteOk
            AMQP$Queue$DeclareOk ShutdownSignalException]
           java.io.IOException
           java.util.UUID
           java.util.concurrent.TimeUnit))


;;
;; exchange.declare
;;

(defonce conn (lhc/connect))

;; direct

(deftest test-declare-a-direct-exchange-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct1"]
    (lhe/declare channel exchange "direct")
    (lhe/delete channel exchange)))

(deftest test-declare-a-direct-exchange-with-default-attributes-shortcut
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct1"]
    (lhe/direct channel exchange)
    (lhe/delete channel exchange)))

(deftest test-declare-a-durable-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct2"]
    (lhe/declare channel exchange "direct" {:auto-delete false :durable true})
    (lhe/delete channel exchange)))

(deftest test-declare-a-durable-direct-exchange-shortcut
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct2"]
    (lhe/direct channel exchange {:auto-delete false :durable true})
    (lhe/delete channel exchange)))

(deftest test-declare-an-auto-deleted-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct3"]
    (lhe/declare channel exchange "direct" {:auto-delete true :durable false})
    (lhe/delete channel exchange)))

(deftest test-declare-an-internal-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct.internal"]
    (lhe/declare channel exchange "direct" {:auto-delete true :durable false :internal true})
    (lhe/delete channel exchange)))


(deftest test-direct-exchange-routing-key-delivery
  (let [conn (lhc/connect)
        channel (lhc/create-channel conn)
        exchange "langohr.tests.exchanges.direct4"
        queue    (lhq/declare-server-named channel)]

    (lhe/declare channel exchange "direct")
    (lhq/bind channel queue exchange {:routing-key "abc"})

    (lhb/publish channel exchange "abc" "")
    (lhb/publish channel exchange "xyz" "")

    (Thread/sleep 200)

    (is (= 1 (lhq/message-count channel queue)))
    (lhe/delete channel exchange)))


;; fanout

(deftest test-declare-a-fanout-exchange-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout1"]
    (lhe/declare channel exchange "fanout")
    (lhe/delete channel exchange)))

(deftest test-declare-a-fanout-exchange-with-default-attributes-shortcut
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout1"]
    (lhe/fanout channel exchange)
    (lhe/delete channel exchange)))

(deftest test-declare-a-durable-fanout-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout2"]
    (lhe/declare channel exchange "fanout" {:durable true})
    (lhe/delete channel exchange)))

(deftest test-declare-a-durable-fanout-exchange-shortcut
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout2"]
    (lhe/fanout channel exchange {:durable true})
    (lhe/delete channel exchange)))

(deftest test-declare-an-auto-deleted-fanout-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.fanout3"]
    (lhe/declare channel exchange "fanout" {:auto-delete true})
    (lhe/delete channel exchange)))


(deftest test-fanount-exchange-broadcast-delivery
  (let [conn (lhc/connect)
        channel (lhc/create-channel conn)
        exchange "langohr.tests.exchanges.fanout4"
        queue    (lhq/declare-server-named channel)]

    (lhe/declare channel exchange "fanout")
    (lhq/bind channel queue exchange)

    (lhb/publish channel exchange "abc" "")
    (lhb/publish channel exchange "xyz" "")
    (Thread/sleep 200)
    (is (= 2 (lhq/message-count channel queue)))
    (lhe/delete channel exchange)))

;; topic

(deftest test-declare-a-topic-exchange-with-default-attributes
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic1"]
    (lhe/declare channel exchange "topic")
    (lhe/delete channel exchange)))

(deftest test-declare-a-topic-exchange-with-default-attributes-shortcut
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic1"]
    (lhe/topic channel exchange)
    (lhe/delete channel exchange)))

(deftest test-declare-a-durable-topic-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic2"]
    (lhe/declare channel exchange "topic" {:durable true})
    (lhe/delete channel exchange)))

(deftest test-declare-a-durable-topic-exchange-shortcut
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic2"]
    (lhe/declare channel exchange "topic" {:durable true})
    (lhe/delete channel exchange)))

(deftest test-declare-an-auto-deleted-topic-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.topic3"]
    (lhe/declare channel exchange "topic" {:auto-delete true})
    (lhe/delete channel exchange)))


(deftest test-redeclare-an-auto-deleted-topic-exchange-with-different-attributes
  (let [conn        (lhc/connect)
        channel     (lhc/create-channel conn)
        exchange    "langohr.tests.exchanges.topic4"
        shutdown-ln (lhc/shutdown-listener (fn [cause]
                                             (println "Shutdown listener has fired")))]
    (try
      (.addShutdownListener channel shutdown-ln)
      (lhe/declare channel exchange "topic" {:auto-delete true})
      (lhe/declare channel exchange "topic" {:auto-delete false})
      (catch IOException ioe ;; see http://www.rabbitmq.com/api-guide.html#shutdown
        (let [tmp-ch (lhc/create-channel conn)]
          (lhe/delete tmp-ch exchange))
        nil))))

(deftest test-topic-exchange-wildcard-delivery
  (let [conn (lhc/connect)
        channel (lhc/create-channel conn)
        exchange "langohr.tests.exchanges.topic5"
        queue    (lhq/declare-server-named channel)]

    (lhe/declare channel exchange "topic")
    (lhq/bind channel queue exchange {:routing-key "log.*"})

    (lhb/publish channel exchange "accounts.signup" "")
    (lhb/publish channel exchange "log.info" "")
    (lhb/publish channel exchange "log.warn" "")

    (Thread/sleep 200)

    (is (= 2 (lhq/message-count channel queue)))

    (lhe/delete channel exchange)))

;; passive declaration

(deftest test-passive-declare-with-existing-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct2"
        _          (lhe/direct channel exchange {:auto-delete false :durable true})
        declare-ok (lhe/declare-passive channel exchange)]
    (is declare-ok)
    (lhe/delete channel exchange)))

(deftest test-passive-declare-with-non-existing-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   (format "langohr.tests.exchanges.%s" (UUID/randomUUID))]
    (is (thrown? java.io.IOException
                 (lhe/declare-passive channel exchange)))))

;;
;; exchange.delete
;;

(deftest test-delete-a-fresh-declared-direct-exchange
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct4"]
    (lhe/declare channel exchange "direct")
    (lhe/delete  channel exchange)))

(deftest test-delete-a-fresh-declared-direct-exchange-if-it-is-unused
  (let [channel    (lhc/create-channel conn)
        exchange   "langohr.tests.exchanges.direct5"]
    (lhe/declare channel exchange "direct")
    (lhe/delete  channel exchange true)))



;;
;; exchange.bind
;;

(deftest test-exchange-bind-without-arguments
  (let [channel     (lhc/create-channel conn)
        source      "langohr.tests.exchanges.source"
        destination "langohr.tests.exchanges.destination"
        queue       (lhq/declare-server-named channel)]
    (lhe/declare channel source      "fanout" {:auto-delete true})
    (lhe/declare channel destination "fanout" {:auto-delete true})
    (lhq/bind channel queue destination)
    (is (nil? (lhb/get channel queue)))
    (lhe/bind    channel destination source)
    (lhb/publish channel source "" "")
    (is (lhb/get channel queue))

    (lhe/delete channel source)
    (lhe/delete channel destination)))

(deftest test-exchange-bind-with-arguments
  (let [channel     (lhc/create-channel conn)
        source      "langohr.tests.exchanges.source2"
        destination "langohr.tests.exchanges.destination2"
        queue       (lhq/declare-server-named channel)]
    (lhe/declare channel source      "fanout" {:auto-delete true})
    (lhe/declare channel destination "fanout" {:auto-delete true})
    (lhq/bind channel queue destination {:arguments {"X-For-Some-Extension" "a value"}})
    (is (nil? (lhb/get channel queue)))
    (lhe/bind    channel destination source)
    (lhb/publish channel source "" "")
    (is (lhb/get channel queue))

    (lhe/delete channel source)
    (lhe/delete channel destination)))


;;
;;  Headers exchange
;;

(deftest test-headers-exchange

  (let [channel (lhc/create-channel conn)
        exchange "langohr.tests.exchanges.headers2"
        queue    (lhq/declare-server-named channel)]

    (lhe/declare channel exchange "headers")
    (lhq/bind channel queue exchange {:arguments {"x-match" "all" "arch" "x86_64" "os" "linux"}})

    (lhb/publish channel exchange "" "For linux/IA64"   {:headers {"arch" "x86_64" "os" "linux"}})
    (lhb/publish channel exchange "" "For linux/x86"    {:headers {"arch" "x86"  "os" "linux"}})
    (lhb/publish channel exchange "" "For any linux"    {:headers {"os" "linux"}})
    (lhb/publish channel exchange "" "For OS X"         {:headers {"os" "macosx"}})
    (lhb/publish channel exchange "" "For solaris/IA64" {:headers {"os" "solaris" "arch" "x86_64"}})

    (Thread/sleep 200)

    (is (= 1 (lhq/message-count channel queue)))

    (lhe/delete channel exchange)))



;;
;; alternate exchanges support
;;

(deftest test-demonstrate-alternate-exchanges-support
  (let [channel     (lhc/create-channel conn)
        fe          "langohr.extensions.altexchanges.fanout1"
        de          "langohr.extensions.altexchanges.direct1"
        queue       (lhq/declare-server-named channel)
        latch       (java.util.concurrent.CountDownLatch. 1)
        msg-handler (fn [ch metadata payload]
                      (.countDown latch))]
    (lhe/declare channel fe "fanout" {:auto-delete true})
    (lhe/declare channel de "direct" {:auto-delete true :arguments {"alternate-exchange" fe}})
    (lhq/bind    channel queue fe)
    (.start (Thread. #(lhcons/subscribe channel queue msg-handler {:auto-ack true}) "subscriber"))
    (.start (Thread. (fn []
                       (lhb/publish channel de "" "1010" {:mandatory true})) "publisher"))
    (is (.await latch 700 TimeUnit/MILLISECONDS))

    (lhe/delete channel fe)
    (lhe/delete channel de)))
