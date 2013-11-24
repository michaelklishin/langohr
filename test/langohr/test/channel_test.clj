(ns langohr.test.channel-test
  (:require [langohr.core    :as lc]
            [langohr.channel :as lch]
            [clojure.test :refer :all]))

(deftest test-open-a-channel
  (let [conn (lc/connect)
        ch   (lch/open conn)]
    (is (instance? com.rabbitmq.client.Channel ch))
    (is (lc/open? ch))))

(deftest test-open-a-channel-with-explicitly-given-id
  (let [conn (lc/connect)
        ch   (lc/create-channel conn 987)]
    (is (instance? com.rabbitmq.client.Channel ch))
    (is (lc/open? ch))
    (is (= (.getChannelNumber ch) 987))
    (lc/close ch)))


(deftest test-close-a-channel-using-langohr-core-close
  (let [conn (lc/connect)
        ch   (lch/open conn)]
    (is (lc/open? ch))
    (lc/close ch)
    (is (lc/closed? ch))))



(deftest test-close-a-channel-using-langohr-channel-close
  (let [conn (lc/connect)
        ch   (lch/open conn)]
    (is (lc/open? ch))
    (lch/close ch)
    (is (lc/closed? ch))))


(deftest test-close-a-channel-using-langohr-channel-close-with-provided-message
  (let [conn (lc/connect)
        ch   (lch/open conn)]
    (is (lc/open? ch))
    (lch/close ch 200 "Bye-bye")
    (is (lc/closed? ch))))



(deftest test-toggle-flow-control
  (let [conn (lc/connect)
        ch   (lch/open conn)]
    (is (lch/flow? ch))
    (lch/flow ch false)
    (lch/flow ch true)
    (is (lch/flow? ch))
    (lch/close ch)
    (is (not (lch/open? ch)))))
