;; Copyright (c) 2011-2025 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.publishing-test
  (:require [langohr.core      :as lhc]
            [langohr.queue     :as lhq]
            [langohr.exchange  :as lhe]
            [langohr.basic     :as lhb]
            [clojure.java.io   :as io]
            [clojure.test      :refer :all])
  (:import com.rabbitmq.client.Connection))

;;
;; Tries to reproduce various edge cases around basic.publish
;;

(defn resource-as-bytes
  [^String path]
  (.getBytes ^String (slurp (io/resource path)) "UTF-8"))

(deftest test-publishing-large-payload1
  (with-open [^Connection conn (lhc/connect)
              ch (lhc/create-channel conn)]
    (let [x     ""
          q     ""
          qd-ok (lhq/declare ch q {:exclusive true})
          body  (resource-as-bytes "payloads/200k_json_payload.json")
          _     (lhb/publish ch x (.getQueue qd-ok) body {:content-type "application-json"})]
      (is (= 247894 (count body)))
      (Thread/sleep 200)
      (let [[_ fetched] (lhb/get ch q)]
        (is fetched)
        (is (= (count body) (count fetched)))))))
