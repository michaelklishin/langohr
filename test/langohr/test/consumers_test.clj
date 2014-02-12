(ns langohr.test.consumers-test
  (:require [langohr.queue     :as lhq]
            [langohr.core      :as lhc]
            [langohr.channel   :as lch]
            [langohr.basic     :as lhb]
            [langohr.util      :as lhu]
            [langohr.consumers :as lhcons]
            [langohr.shutdown  :as lsh]
            [clojure.test      :refer :all])
  (:import [com.rabbitmq.client Connection Consumer]
           [java.util.concurrent TimeUnit CountDownLatch]))


;;
;; API
;;

(deftest t-consume-ok-handler
  (with-open [^Connection conn (lhc/connect)
              channel          (lch/open conn)]
    (let [queue    (lhq/declare-server-named channel)
          latch    (CountDownLatch. 1)
          consumer (lhcons/create-default channel :handle-consume-ok-fn (fn [consumer-tag]
                                                                          (.countDown latch)))]
      (lhb/consume channel queue consumer)
      (is (.await latch 1000 TimeUnit/MILLISECONDS)))))

(deftest t-basic-cancel
  (with-open [^Connection conn (lhc/connect)
              ch               (lch/open conn)]
    (let [q     (lhq/declare-server-named ch)
          ctag  "langohr.consumer-tag1"
          latch (CountDownLatch. 1)]
      (lhcons/subscribe ch q (fn [_ _ _])
                        :consumer-tag ctag :handle-cancel-ok (fn [_]
                                                               (.countDown latch)))
      (lhb/cancel ch ctag)
      (is (.await latch 1000 TimeUnit/MILLISECONDS)))))

(deftest t-basic-cancel-with-exclusive-consumer
  (with-open [^Connection conn (lhc/connect)
              ch               (lch/open conn)]
    (let [q     (lhq/declare-server-named ch)
          ctag  "langohr.consumer-tag"
          latch (CountDownLatch. 1)]
      (lhcons/subscribe ch q (fn [_ _ _])
                        :consumer-tag ctag :handle-cancel-ok (fn [_]
                                                               (.countDown latch))
                        :exclusive true)
      (lhb/cancel ch ctag)
      (is (.await latch 1000 TimeUnit/MILLISECONDS)))))

(deftest t-basic-cancel-with-exclusive-consumer-on-different-connection
  (with-open [^Connection conn (lhc/connect)
              ch               (lch/open conn)
              conn2            (lhc/connect)
              ch2              (lch/open conn2)]
    (let [q     (:queue (lhq/declare ch "" :exclusive false))
          ctag  "langohr.consumer-tag"]
      (lhcons/subscribe ch q (fn [_ _ _])
                        :consumer-tag ctag :exclusive true)
      (is (thrown? java.io.IOException
                   (lhb/cancel ch2 ctag)))
      (try
        (lhb/cancel ch2 ctag)
        (catch java.io.IOException e
          (is (= (.getMessage e) "Unknown consumerTag")))))))

(deftest t-consume-ok-handler-with-queueing-consumer
  (with-open [^Connection conn (lhc/connect)
              channel          (lch/open conn)]
    (let [queue    (:queue (lhq/declare channel))
          latch    (CountDownLatch. 1)
          consumer (lhcons/create-queueing channel :handle-consume-ok-fn (fn [consumer-tag]
                                                                           (.countDown latch)))]
      (lhb/consume channel queue consumer)
      (is (.await latch 700 TimeUnit/MILLISECONDS)))))


(deftest t-cancel-ok-handler
  (with-open [^Connection conn (lhc/connect)
              channel          (lch/open conn)]
    (let [queue    (lhq/declare-server-named channel)
          tag      (lhu/generate-consumer-tag "t-cancel-ok-handler")
          latch    (CountDownLatch. 1)
          consumer (lhcons/create-default channel :handle-cancel-ok-fn (fn [consumer-tag]
                                                                         (.countDown latch)))
          ret-tag  (lhb/consume channel queue consumer :consumer-tag tag)]
      (is (= tag ret-tag))
      (lhb/cancel channel tag)
      (is (.await latch 2 TimeUnit/SECONDS)))))


(deftest t-cancel-notification-handler
  (with-open [^Connection conn (lhc/connect)
              channel          (lch/open conn)]
    (let [queue    "to-be-deleted"
          latch    (CountDownLatch. 1)
          consumer (lhcons/create-default channel :handle-cancel-fn (fn [consumer_tag]
                                                                      (.countDown latch)))]
      (lhq/declare channel queue)
      (lhb/consume channel queue consumer :consumer-tag "will-be-cancelled-by-rmq")
      (lhq/delete channel queue)
      (is (.await latch 1 TimeUnit/SECONDS))
      (lhq/delete channel queue))))


(deftest t-delivery-handler
  (with-open [^Connection conn (lhc/connect)
              channel          (lch/open conn)]
    (let [exchange   ""
          payload    ""
          queue      (lhq/declare-server-named channel)
          n          300
          latch      (CountDownLatch. (inc n))
          consumer   (lhcons/create-default channel
                                            :handle-delivery-fn   (fn [ch metadata ^bytes payload]
                                                                    (.countDown latch))
                                            :handle-consume-ok-fn (fn [consumer-tag]
                                                                    (.countDown latch)))]
      (lhb/consume channel queue consumer)
      (dotimes [i n]
        (lhb/publish channel exchange queue payload))
      (is (.await latch 700 TimeUnit/MILLISECONDS)))))

(deftest t-shutdown-notification-handler
  (with-open [^Connection conn (lhc/connect)]
    (let [ch       (lch/open conn)
          q        (lhq/declare-server-named ch)
          latch    (CountDownLatch. 1)
          consumer (lhcons/create-default ch :handle-shutdown-signal-fn (fn [consumer_tag reason]
                                                                          (.countDown latch)))]
      (lhb/consume ch q consumer)
      ;; bind a queue that does not exist
      (try
        (lhq/bind ch "ugggggh" "amq.fanout")
        (catch Exception e
          (comment "Ignore")))
      (is (.await latch 700 TimeUnit/MILLISECONDS)))))
