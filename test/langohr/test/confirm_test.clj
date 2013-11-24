(ns langohr.test.confirm-test
  (:import [com.rabbitmq.client Connection AMQP$Queue$DeclareOk AMQP$Confirm$SelectOk])
  (:require langohr.confirm
            [langohr.core    :as lhc]
            [langohr.channel :as lch]
            [langohr.basic   :as lhb]
            [langohr.queue   :as lhq]
            [clojure.test    :refer :all]))

(defonce ^Connection conn (lhc/connect))

(deftest t-confirm-select
  (let [channel (lhc/create-channel conn)]
    (is (instance? AMQP$Confirm$SelectOk (langohr.confirm/select channel)))))

(deftest t-confirm-select-with-a-listener
  (let [channel  (lhc/create-channel conn)
        queue    (.getQueue (lhq/declare channel))
        latch    (java.util.concurrent.CountDownLatch. 1)
        listener (langohr.confirm/listener (fn [delivery-tag, multiple]
                                             (.countDown latch))
                                           (fn [delivery-tag, multiple]
                                             (.countDown latch)))]
    (langohr.confirm/select channel)
    (langohr.confirm/add-listener channel listener)
    (.start (Thread. (fn []
                       (lhb/publish channel "" queue "")) "publisher"))
    (.await latch)))


(deftest t-confirm-select-with-callback-functions
  (let [channel  (lhc/create-channel conn)
        queue    (.getQueue (lhq/declare channel))
        latch    (java.util.concurrent.CountDownLatch. 1)]
    (langohr.confirm/select channel
                            (fn [delivery-tag, multiple]
                              (.countDown latch))
                            (fn [delivery-tag, multiple]
                              (.countDown latch)))
    (.start (Thread. (fn []
                       (lhb/publish channel "" queue "")) "publisher"))
    (.await latch)))

(deftest test-publishing-confirms
  (with-open [conn (lhc/connect)
              ch   (lhc/create-channel conn)]
    (let [x     ""
          q     "langohr.publisher.confirms"
          _     (lhq/declare ch q :exclusive true)
          body  (.getBytes "message" "UTF-8")]
      (langohr.confirm/select ch)
      (lhb/publish ch x q body :content-type "application-json")
      (langohr.confirm/wait-for-confirms ch 200)
      (is true))))
