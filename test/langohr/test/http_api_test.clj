;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.http-api-test
  (:require [langohr.http :as hc]
            [clojure.test :refer :all]
            [clojure.set :refer [subset? superset?]]
            [langohr.core    :as rmq]
            [langohr.channel :as lch]
            [langohr.queue   :as lq]))

(hc/connect! "http://127.0.0.1:15672" "guest" "guest")

;;
;; These tests are pretty basic and make sure we don't
;; have any obvious issues. The results are returns as
;; JSON responses parsed into Clojure maps.
;;

(deftest ^{:http true} test-get-overview
  (let [r (hc/get-overview)]
    (is (get-in r [:queue_totals :messages]))
    (is (get-in r [:queue_totals :messages_ready]))
    (is (get-in r [:queue_totals :messages_unacknowledged]))
    (is (get-in r [:queue_totals :messages_details :rate]))))

(deftest ^{:http true} test-list-nodes
  (let [r (hc/list-nodes)
        n (first r)]
    (is (coll? r))
    (doseq [keys [[:proc_total]
                  [:disk_free]
                  [:sockets_total]
                  [:sockets_used]]]
      (is (get-in n keys)))))

(deftest ^{:http true} test-list-extensions
  (let [r (hc/list-extensions)
        e (first r)]
    (is (get e :javascript))))

(deftest ^{:http true} test-list-definitions
  (with-open [conn (rmq/connect)
              ch   (lch/open conn)]
    (dotimes [i 100]
      (lq/declare-server-named ch))
    (let [r           (hc/list-definitions)
          vhosts      (:vhosts r)
          exchanges   (:exchanges r)
          parameters  (:parameters r)
          policies    (:policies r)
          permissions (:permissions r)]
      (is (:rabbit_version r))
      (is (:name (first vhosts)))
      (is (:name (first queues)))
      (is (:name (first exchanges)))
      (is (:source (first bindings)))
      (is (:user (first permissions))))))

(deftest ^{:http true} test-list-connections
  (let [r (hc/list-connections)]
    (is (coll? r))))

(deftest ^{:http true} test-list-channels
  (let [r (hc/list-channels)]
    (is (coll? r))))

(deftest ^{:http true} test-list-exchanges
  (let [r (hc/list-exchanges)]
    (is (coll? r)))
  (let [r (hc/list-exchanges "/")]
    (is (coll? r))))

(deftest ^{:http true} test-get-exchange
  (let [r (hc/get-exchange "/" "amq.fanout")]
    (is (:name r))
    (is (:vhost r))
    (is (:type r))
    (is (:durable r))
    (is (:arguments r))))

(deftest ^{:http true} test-get-non-existing-vhost
  (let [response (hc/list-exchanges "amq.non-existing-vhost")]
                                        ; since this vhost does not exist, there is no JSON response, just the body as a string
    (is (string? response))))

(deftest ^{:http true} test-declare-and-delete-exchange
  (let [s  "langohr.http.fanout"
        r1 (hc/declare-exchange "/" s {:durable false :auto_delete true :internal false :arguments {}})
        r2 (hc/delete-exchange "/" s)]
    (is (= true r1))
    (is (= true r2))))

(deftest ^{:http true} test-list-queues
  (let [r (hc/list-queues)]
    (is (coll? r)))
  (let [r (hc/list-queues "/")]
    (is (coll? r))))

(deftest ^{:http true} test-declare-and-delete-queue
  (let [s  "langohr.http.queue"
        r1 (hc/declare-queue "/" s {:durable false :auto_delete true :arguments {}})
        r2 (hc/delete-queue "/" s)]
    (is (= true r1))
    (is (= true r2))))


(deftest ^{:http true} test-declare-and-purge-queue
  (let [s  "langohr.http.queue"
        r1 (hc/declare-queue "/" s {:durable false :auto_delete true :arguments {}})
        r2 (hc/purge-queue "/" s)
        _  (hc/delete-queue "/" s)]
    (is (= true r1))
    (is (= true r2))))

(deftest ^{:http true} test-list-bindings
  (let [q  "langohr.http.queue"
        e  "langohr.http.fanout"
        _ (hc/declare-exchange "/" e {:durable false :auto_delete true :internal false :arguments {}})
        _ (hc/declare-queue "/" q {:durable false :auto_delete true :arguments {}})
        r1 (hc/bind "/" e q)
        xs (hc/list-bindings "/")
        m  (first xs)]
    (is r1)
    (is (coll? xs))
    (is (:source m))
    (is (:destination m))
    (is (:vhost m))
    (is (= "queue" (:destination_type m)))))

(deftest ^{:http true} test-list-vhosts
  (let [xs (hc/list-vhosts)]
    (is (subset? #{"/"} (set (map :name xs))))))

(deftest ^{:http true} test-vhost-manipulations
  (let [vhost "new-vhost"]
    (is (not (hc/vhost-exists? vhost)))
    (hc/add-vhost vhost)
    (is (hc/vhost-exists? vhost))
    (hc/delete-vhost vhost)))

(deftest ^{:http true} test-user-manipulations
  (let [user "a-new-user"]
    (is (not (hc/user-exists? user)))
    (is (hc/add-user user "password" ""))
    (is (= user (:name (hc/get-user user))))
    (is (hc/user-exists? user))
    (is (some #{user} (map :name (hc/list-users))))
    (is (hc/delete-user user))))

(deftest ^{:http true} test-permissions-manipulations
  (let [user "a-new-user"
        vhost "/"
        permissions {:configure ".*" :write "write-only-exchange" :read "a|b|c"}]
    (is (hc/add-user user "password" ""))
    (is (hc/set-permissions vhost user permissions))
    (is (= permissions (select-keys (hc/get-permissions vhost user) (keys permissions))))
    (is (hc/delete-user user))))
