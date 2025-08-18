;; Copyright (c) 2011-2025 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.basic-test
  (:refer-clojure :exclude [get declare])
  (:require [langohr.core      :as lhc]
            [langohr.consumers :as lhcons]
            [langohr.queue     :as lhq]
            [langohr.exchange  :as lhe]
            [langohr.basic     :as lhb]
            [langohr.util      :as lhu]
            [clojure.test :refer [deftest is]])
  (:import [com.rabbitmq.client Connection Channel AMQP
            AMQP$BasicProperties AMQP$BasicProperties$Builder
            GetResponse AMQP$Queue$DeclareOk]
           java.util.UUID
           java.util.concurrent.TimeUnit))

;;
;; basic.publish, basic.consume
;;

(deftest test-publishing-using-default-exchange-and-default-message-attributes
  (with-open [^Connection conn (lhc/connect)
              channel          (lhc/create-channel conn)]
    (let [exchange   ""
          ;; yes, payload may be blank. This is an edge case Ruby amqp
          ;; gem did not support for a long time so I want to use it in the langohr
          ;; test suite. MK.
          payload    ""
          queue      "langohr.examples.publishing.using-default-exchange"
          declare-ok (lhq/declare channel queue {:auto-delete true})
          tag        (lhu/generate-consumer-tag "langohr.basic/consume-tests")
          content-type "text/plain"
          msg-id       (.toString (java.util.UUID/randomUUID))
          n            3000
          latch        (java.util.concurrent.CountDownLatch. n)
          msg-handler   (fn [ch metadata payload]
                          (is (:delivery-tag metadata))
                          (is (:content-type metadata))
                          (is (:headers metadata))
                          (is (:message-id metadata))
                          (is (:priority metadata))
                          (.countDown latch))]
      (lhcons/subscribe channel queue msg-handler {:consumer-tag tag :auto-ack true})
      (.start (Thread. (fn []
                         (dotimes [i n]
                           (lhb/publish channel exchange queue payload
                                        {:priority 8
                                         :message-id msg-id
                                         :content-type content-type
                                         :headers {"see you soon" "à bientôt"}}))) "publisher"))
      (is (.await latch 3 TimeUnit/SECONDS)))))

;;
;; make sure that `langohr.consumers/subscribe` takes both versions for handler functions:
;; for example `:handle-consume-ok` as well as `:handle-consume-ok-fn`.
;;

(deftest test-subscribe-with-custom-handler
  (with-open [^Connection conn (lhc/connect)
              channel          (lhc/create-channel conn)]
    (let [exchange   ""
          queue (lhq/declare-server-named channel)
          latch        (java.util.concurrent.CountDownLatch. 1)
          handler-called (atom #{})
          msg-handler   (fn [ch metadata payload]
                          (.countDown latch))
          log-called (fn [tag] (fn [_] (swap! handler-called conj tag)))]
      (lhcons/subscribe channel queue msg-handler {:auto-ack true :handle-consume-ok-fn (log-called :handle-consume-ok-fn)})
      (lhcons/subscribe channel queue msg-handler {:auto-ack true :handle-consume-ok (log-called :handle-consume-ok)})
      (lhb/publish channel exchange queue "dummy payload")
      (is (.await latch 700 TimeUnit/MILLISECONDS))
      (is (= #{:handle-consume-ok-fn :handle-consume-ok} @handler-called)))))


(deftest test-demonstrate-sender-selected-distribution-extension-support
  (with-open [^Connection conn (lhc/connect)
              channel          (lhc/create-channel conn)]
    (let [queue1      (lhq/declare-server-named channel)
          queue2      (lhq/declare-server-named channel)
          queue3      (lhq/declare-server-named channel)]
      (lhb/publish channel "" queue1 "1010" {:headers {"CC" [queue2] "BCC" [queue3]}})
      (is (lhb/get channel queue1))
      (is (lhb/get channel queue2))
      (is (lhb/get channel queue3)))))




;;
;; basic.cancel
;;

(deftest test-basic-cancel
  (with-open [^Connection conn (lhc/connect)
              channel          (lhc/create-channel conn)]
    (let [exchange    ""
          payload     ""
          queue       (.getQueue (lhq/declare channel "" {:auto-delete true}))
          tag         (lhu/generate-consumer-tag "langohr.basic/consume-tests")
          counter     (atom 0)
          msg-handler (fn [ch metadata payload]
                        (swap! counter inc))]
      (lhcons/subscribe channel queue msg-handler {:consumer-tag tag :auto-ack true})
      (lhb/publish channel exchange queue payload)
      (Thread/sleep 200)
      (is (= @counter 1))
      (lhb/cancel channel tag)
      (dotimes [i 50]
        (lhb/publish channel exchange queue payload))
      (Thread/sleep 200)
      (is (= @counter 1)))))

;;
;; basic.get
;;

(deftest test-basic-get-with-automatic-ack
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [exchange   ""
          body       "A message we will fetch with basic.get"
          queue      "langohr.examples.basic.get.queue1"]
      (lhq/declare ch queue {:auto-delete true})
      (lhb/publish ch exchange queue body)
      (let [[metadata payload] (lhb/get ch queue)]
        (is (= (String. ^bytes payload) body))
        (is (= (:message-count metadata) 0))
        (is (= (:exchange metadata) exchange))
        (is (= (:routing-key metadata) queue)))
      (lhq/delete ch queue))))

(deftest test-basic-get-with-explicit-ack
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [exchange   ""
          body       "A message we will fetch with basic.get"
          queue      "langohr.examples.basic.get.queue2"]
      (lhq/declare ch queue)
      (lhb/publish ch exchange queue body)
      (let [[metadata payload] (lhb/get ch queue false)]
        (is (= (String. ^bytes payload) body))
        (lhb/ack ch (:delivery-tag metadata)))
      (lhq/delete ch queue))))


(deftest test-basic-get-with-an-empty-queue
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [queue (lhq/declare-server-named ch)]
      (is (nil? (lhb/get ch queue false))))))

;;
;; basic.qos
;;

(deftest test-using-non-global-basic-qos
  (with-open [^Connection conn (lhc/connect)
              ch               (lhc/create-channel conn)]
    (lhb/qos ch 5)))

;;
;; basic.ack
;;

(deftest test-acknowledge-one-message
  (with-open [^Connection conn (lhc/connect)
              producer-ch (lhc/create-channel conn)
              consumer-ch (lhc/create-channel conn)]
    (let [queue "langohr.examples.basic.ack.queue1"]
      (lhq/declare consumer-ch queue)
      (lhq/purge   producer-ch queue)
      (.start (Thread. (fn []
                         (lhb/publish producer-ch "" queue "One")
                         (lhb/publish producer-ch "" queue "Two")
                         (lhb/publish producer-ch "" queue "Three"))))
      (Thread/sleep 200)
      (let [[{delivery-tag :delivery-tag} _] (lhb/get consumer-ch queue false)]
        (is (= 1 delivery-tag))
        (lhb/ack consumer-ch delivery-tag))
      (lhq/delete consumer-ch queue))))

(deftest test-acknowledge-multiple-messages
  (with-open [^Connection conn (lhc/connect)
              producer-ch (lhc/create-channel conn)
              consumer-ch (lhc/create-channel conn)]
    (let [queue "langohr.examples.basic.ack.queue2"]
      (lhq/declare consumer-ch queue)
      (lhq/purge   producer-ch queue)
      (.start (Thread. (fn []
                         (lhb/publish producer-ch "" queue "One")
                         (lhb/publish producer-ch "" queue "Two")
                         (lhb/publish producer-ch "" queue "Three"))))
      (Thread/sleep 200)
      (let [[{delivery-tag1 :delivery-tag} _] (lhb/get consumer-ch queue false)
            [{delivery-tag2 :delivery-tag} _] (lhb/get consumer-ch queue false)]
        (is (= 1 delivery-tag1))
        (is (= 2 delivery-tag2))
        (lhb/ack consumer-ch delivery-tag1 true))
      (lhq/delete producer-ch queue))))

;;
;; basic.nack
;;

(deftest test-nack-one-message-to-requeue-it
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [queue "langohr.examples.basic.nack.queue1"]
      (lhq/declare ch queue)
      (lhq/purge ch queue)
      (.start (Thread. (fn []
                         (lhb/publish ch "" queue "One")
                         (lhb/publish ch "" queue "Two")
                         (lhb/publish ch "" queue "Three"))))
      (Thread/sleep 200)
      (let [[{:keys [delivery-tag]} _] (lhb/get ch queue false)]
        (is (= 1 delivery-tag))
        (lhb/nack ch delivery-tag false true))
      (lhq/delete ch queue))))

(deftest test-nack-multiple-messages-without-requeueing
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [queue "langohr.examples.basic.nack.queue2"]
      (lhq/declare ch queue)
      (lhq/purge ch queue)
      (.start (Thread. (fn []
                         (lhb/publish ch "" queue "One")
                         (lhb/publish ch "" queue "Two")
                         (lhb/publish ch "" queue "Three"))))
      (Thread/sleep 200)
      (let [[{delivery-tag1 :delivery-tag} _] (lhb/get ch queue false)
            [{delivery-tag2 :delivery-tag} _] (lhb/get ch queue false)]
        (is (= 1 delivery-tag1))
        (is (= 2 delivery-tag2))
        (lhb/nack ch delivery-tag1 true false))
      (lhq/delete ch queue))))

;;
;; basic.reject
;;

(deftest test-reject-one-message-to-requeue-it
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [queue "langohr.examples.basic.reject.queue1"]
      (lhq/declare ch queue)
      (lhq/purge ch queue)
      (.start (Thread. (fn []
                         (lhb/publish ch "" queue "One")
                         (lhb/publish ch "" queue "Two")
                         (lhb/publish ch "" queue "Three"))))
      (Thread/sleep 200)
      (let [[{:keys [delivery-tag]} _] (lhb/get ch queue false)]
        (is (= 1 delivery-tag))
        (lhb/reject ch delivery-tag true))
      (lhq/delete ch queue))))

(deftest test-reject-one-message-without-requeueing
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [queue "langohr.examples.basic.reject.queue2"]
      (lhq/declare ch queue)
      (.start (Thread. (fn []
                         (lhb/publish ch "" queue "One")
                         (lhb/publish ch "" queue "Two")
                         (lhb/publish ch "" queue "Three"))))
      (Thread/sleep 200)
      (let [[{delivery-tag1 :delivery-tag} _] (lhb/get ch queue false)
            [{delivery-tag2 :delivery-tag} _] (lhb/get ch queue false)]
        (is (= 1 delivery-tag1))
        (is (= 2 delivery-tag2))
        (lhb/reject ch delivery-tag1 false))
      (lhq/delete ch queue))))

;;
;; basic.return
;;

(deftest test-handling-of-returned-mandatory-messages-with-a-listener-instance
  (with-open [^Connection conn (lhc/connect)
              ch          (lhc/create-channel conn)]
    (let [exchange "langohr.tests.basic.return1"
          latch    (java.util.concurrent.CountDownLatch. 1)
          rl       (fn [reply-code reply-text exchange routing-key properties body]
                     (is (= reply-text "NO_ROUTE"))
                     (is (= (String. ^bytes body) "return-me"))
                     (.countDown latch))]
      (lhb/add-return-listener ch rl)
      (lhe/declare ch exchange "direct" {:auto-delete true})
      (lhb/publish ch exchange (str (UUID/randomUUID)) "return-me" {:mandatory true})
      (is (.await latch 1 TimeUnit/SECONDS))
      (lhe/delete ch exchange))))
