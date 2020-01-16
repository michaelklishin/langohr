;; Copyright (c) 2011-2020 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.confirm-test
  (:require [langohr.core    :as lhc]
            [langohr.channel :as lch]
            [langohr.basic   :as lhb]
            [langohr.queue   :as lhq]
            [langohr.confirm :as cfm]
            [clojure.test    :refer :all])
  (:import [com.rabbitmq.client Connection AMQP$Queue$DeclareOk AMQP$Confirm$SelectOk]
           [java.util.concurrent CountDownLatch TimeUnit]))

(deftest t-confirm-select-with-a-listener
  (with-open [^Connection conn (lhc/connect)
              channel          (lhc/create-channel conn)]
    (let [queue    (lhq/declare-server-named channel)
          latch    (CountDownLatch. 1)
          listener (cfm/listener (fn [delivery-tag, multiple]
                                               (.countDown latch))
                                             (fn [delivery-tag, multiple]
                                               (.countDown latch)))]
      (cfm/select channel)
      (cfm/add-listener channel listener)
      (lhb/publish channel "" queue "")
      (is (.await latch 700 TimeUnit/MILLISECONDS)))))


(deftest t-confirm-select-with-callback-functions
  (with-open [^Connection conn (lhc/connect)
              channel          (lhc/create-channel conn)]
    (let [queue    (lhq/declare-server-named channel)
          latch    (CountDownLatch. 1)]
      (cfm/select channel
                  (fn [delivery-tag, multiple]
                    (.countDown latch))
                  (fn [delivery-tag, multiple]
                    (.countDown latch)))
      (lhb/publish channel "" queue "")
      (is (.await latch 700 TimeUnit/MILLISECONDS)))))

(deftest test-publishing-confirms
  (with-open [conn (lhc/connect)
              ch   (lhc/create-channel conn)]
    (let [x     ""
          q     "langohr.publisher.confirms"
          _     (lhq/declare ch q {:exclusive true})
          body  (.getBytes "message" "UTF-8")]
      (cfm/select ch)
      (lhb/publish ch x q body {:content-type "application-json"})
      (cfm/wait-for-confirms ch 200)
      (is true))))
