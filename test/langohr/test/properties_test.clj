;; Copyright (c) 2011-2020 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.properties-test
  "Connection recovery tests"
  (:refer-clojure :exclude [await])
  (:require [langohr.core     :as rmq]
            [langohr.http    :as mgmt]
            [clojure.test :refer :all]))

;;
;; Helpers
;;

(defn await-event-propagation
  "Gives management plugin stats database a chance to update
   (updates happen asynchronously)"
  []
  (Thread/sleep 1150))

;;
;; Tests
;;

(deftest test-connection-name
  (with-open [conn (rmq/connect {:connection-name "George"})]
    (is (rmq/open? conn))
    (await-event-propagation)
    (let [conns (mgmt/list-connections)]
      (is (= 1 (count conns)))
      (is (= "George" (-> conns first :user_provided_name))))))
