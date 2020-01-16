;; Copyright (c) 2011-2020 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.channel-test
  (:require [langohr.core    :as lc]
            [langohr.channel :as lch]
            [clojure.test :refer :all])
  (:import com.rabbitmq.client.Connection))

(deftest test-open-a-channel
  (with-open [^Connection conn (lc/connect)]
    (let [ch   (lch/open conn)]
      (is (instance? com.rabbitmq.client.Channel ch))
      (is (lc/open? ch)))))

(deftest test-open-a-channel-with-explicitly-given-id
  (with-open [^Connection conn (lc/connect)]
    (let [ch   (lc/create-channel conn 987)]
      (is (instance? com.rabbitmq.client.Channel ch))
      (is (lc/open? ch))
      (is (= (.getChannelNumber ch) 987))
      (lc/close ch))))

(deftest test-close-a-channel-using-langohr-core-close
  (with-open [^Connection conn (lc/connect)]
    (let [ch   (lch/open conn)]
      (is (lc/open? ch))
      (lc/close ch)
      (is (lc/closed? ch)))))

(deftest test-close-a-channel-using-langohr-channel-close
  (with-open [^Connection conn (lc/connect)]
    (let [ch   (lch/open conn)]
      (is (lc/open? ch))
      (lch/close ch)
      (is (lc/closed? ch)))))

(deftest test-close-a-channel-using-langohr-channel-close-with-provided-message
  (with-open [^Connection conn (lc/connect)]
    (let [ch   (lch/open conn)]
      (is (lc/open? ch))
      (lch/close ch 200 "Bye-bye")
      (is (lc/closed? ch)))))
