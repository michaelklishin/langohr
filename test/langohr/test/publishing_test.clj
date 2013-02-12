(ns langohr.test.publishing-test
  (:use clojure.test)
  (:require [langohr.core      :as lhc]
            [langohr.queue     :as lhq]
            [langohr.exchange  :as lhe]
            [langohr.basic     :as lhb]
            [clojure.java.io   :as io]))

;;
;; Tries to reproduce various edge cases around basic.publish
;;

(defonce conn (lhc/connect))

(defn resource-as-bytes
  [^String path]
  (.getBytes (slurp (io/resource path)) "UTF-8"))

(deftest test-publishing-large-payload1
  (with-open [ch (lhc/create-channel conn)]
    (let [x     ""
          q     ""
          qd-ok (lhq/declare ch q :exclusive true)
          body  (resource-as-bytes "payloads/200k_json_payload.json")
          _     (lhb/publish ch x (.getQueue qd-ok) body :content-type "application-json")]
      (is (= 247894 (count body)))
      (Thread/sleep 200)
      (let [[_ fetched] (lhb/get ch q)]
        (is fetched)
        (is (= (count body) (count fetched)))))))
