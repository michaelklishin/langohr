;; This source code is dual-licensed under the Apache License, version
;; 2.0, and the Eclipse Public License, version 1.0.
;;
;; The APL v2.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;; ----------------------------------------------------------------------------------
;;
;; The EPL v1.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;; All rights reserved.
;;
;; This program and the accompanying materials are made available under the terms of
;; the Eclipse Public License Version 1.0,
;; which accompanies this distribution and is available at
;; http://www.eclipse.org/legal/epl-v10.html.
;; ----------------------------------------------------------------------------------

(ns langohr.http
  "RabbitMQ HTTP API client.

   Relevant documentation guides:

   * http://www.rabbitmq.com/management.html
   * https://raw.githack.com/rabbitmq/rabbitmq-management/rabbitmq_v3_5_6/priv/www/api/index.html"
  (:refer-clojure :exclude [get])
  (:require [clj-http.client :as http]
            [cheshire.core   :as json]
            [clojure.string  :as s])
  (:import java.net.URLEncoder))

;;
;; Implementation
;;

(def ^:dynamic *endpoint* "http://127.0.0.1:15672")

(def ^:dynamic *username* "guest")
(def ^:dynamic *password* "guest")
(def ^:const throw-exceptions false)

(def ^:dynamic *default-http-options* {:accept :json
                                       :content-type "application/json"
                                       :throw-exceptions throw-exceptions})

(defn url-with-path
  [path]
  (str *endpoint* path))

(defn safe-json-decode
  "Try to parse json response. If the content-type is not json, just return the body (string)."
  [{body :body {content-type "content-type"} :headers}]
  (if (or (nil? body) (.isEmpty ^String body))
    nil
    (if (.contains (.toLowerCase ^String content-type) "json")
      (json/decode body true)
      body)))

(defn ^{:private true} post
  ([^String uri]
     (post uri {}))
  ([^String uri {:keys [body] :as options}]
     (io! (:body (http/post uri (merge *default-http-options* options {:basic-auth [*username* *password*]
                                                                       :body (json/encode body)}))) true)))

(defn ^{:private true} put
  [^String uri {:keys [body] :as options}]
  (io! (:body (http/put uri (merge *default-http-options* options {:basic-auth [*username* *password*]
                                                                   :body (json/encode body)}))) true))

(defn ^{:private true} get
  ([^String uri]
     (get uri {}))
  ([^String uri options]
     (io! (http/get uri (merge *default-http-options* options {:basic-auth [*username* *password*]})))))

(defn ^{:private true} get-and-decode-json
  ([^String uri]
     (safe-json-decode (get uri)))
  ([^String uri options]
     (safe-json-decode (get uri options))))

(defn ^{:private true} head
  ([^String uri]
     (head uri {}))
  ([^String uri options]
     (io! (http/head uri (merge *default-http-options* options {:basic-auth [*username* *password*]})))))

(defn ^{:private true} delete
  ([^String uri]
     (delete uri {}))
  ([^String uri {:keys [body] :as options}]
     (io! (:body (http/delete uri (merge *default-http-options* options {:basic-auth [*username* *password*]
                                                                         :body (json/encode body)}))) true)))

(defn ^{:private true} missing?
  [status]
  (= status 404))

(defn ^{:private true} normalize-protocol
  [proto]
  (case proto
    "amqp/ssl" "amqps"
    (.toLowerCase ^String (str proto))))

;;
;; API
;;

(defn connect!
  ([^String endpoint ^String username ^String password]
     (connect! endpoint username password {}))
  ([^String endpoint ^String username ^String password opts]
     (alter-var-root (var *endpoint*) (constantly endpoint))
     (alter-var-root (var *username*) (constantly username))
     (alter-var-root (var *password*) (constantly password))
     (alter-var-root (var *default-http-options*) (fn [m]
                                                    (merge m opts)))))


(defn get-overview
  ([]
     (get-overview {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/overview") m)))

(defn list-enabled-protocols
  ([]
     (list-enabled-protocols {}))
  ([m]
     (set (map :protocol (:listeners (get-overview m))))))

(defn protocol-ports
  ([]
     (protocol-ports {}))
  ([m]
     (let [xs (:listeners (get-overview m))]
       (reduce (fn [acc lnr]
                 (assoc acc (normalize-protocol (:protocol lnr)) (:port lnr)))
               {}
               xs))))

(defn list-nodes
  ([]
     (list-nodes {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/nodes") m)))

(defn get-node
  ([]
     (get-node {}))
  ([^String node m]
     (get-and-decode-json (url-with-path (str "/api/nodes/" node)) m)))


(defn list-extensions
  ([]
     (list-extensions {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/extensions") m)))


(defn list-definitions
  ([]
     (list-definitions {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/definitions") m)))

(defn list-connections
  ([]
     (list-connections {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/connections") m)))

(defn list-connections-from
  ([^String user]
     (list-connections-from user {}))
  ([^String user m]
     (let [xs (list-connections m)]
       (filter (fn [m]
                 (= user (:user m)))
               xs))))

(defn get-connection
  ([^String id]
     (get-connection id {}))
  ([^String id m]
     (get-and-decode-json (url-with-path (str "/api/connections/" id)) m)))

(defn close-connection
  ([^String id]
     (close-connection id {}))
  ([^String id m]
     (delete (url-with-path (str "/api/connections/" id)) m)))

(defn close-connections-from
  ([^String user]
     (close-connections-from user {}))
  ([^String user m]
     (doseq [^String cn (map :name (list-connections-from user m))]
       (close-connection cn m))))

(defn list-channels
  ([]
     (list-channels {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/channels") m)))

(defn list-exchanges
  ([]
     (get-and-decode-json (url-with-path "/api/exchanges")))
  ([^String vhost]
     (get-and-decode-json (url-with-path (str "/api/exchanges/" (URLEncoder/encode vhost))))))

(defn get-exchange
  [^String vhost ^String exchange]
  (get-and-decode-json (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn declare-exchange
  [^String vhost ^String exchange properties]
  (put (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange))) {:body properties}))

(defn delete-exchange
  ([^String vhost ^String exchange]
     (delete-exchange vhost exchange {}))
  ([^String vhost ^String exchange m]
     (delete (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange))) m)))

(defn list-bindings-for-which-exchange-is-the-source
  [^String vhost ^String exchange]
  (get-and-decode-json (url-with-path (format "/api/exchanges/%s/%s/bindings/source" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn list-bindings-for-which-exchange-is-the-destination
  [^String vhost ^String exchange]
  (get-and-decode-json (url-with-path (format "/api/exchanges/%s/%s/bindings/destination" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn publish
  [^String vhost ^String exchange]
  )

(defn list-queues
  ([]
     (get-and-decode-json (url-with-path "/api/queues")))
  ([^String vhost]
     (list-queues vhost {}))
  ([^String vhost m]
     (get-and-decode-json (url-with-path (format "/api/queues/%s" (URLEncoder/encode vhost))) m)))

(defn get-queue
  [^String vhost ^String queue]
  (get-and-decode-json (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue)))))

(defn declare-queue
  [^String vhost ^String queue properties]
  (put (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue))) {:body properties}))

(defn delete-queue
  ([^String vhost ^String queue]
     (delete-queue vhost queue {}))
  ([^String vhost ^String queue m]
     (delete (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue))) m)))

(defn purge-queue
  ([^String vhost ^String queue]
     (purge-queue vhost queue {}))
  ([^String vhost ^String queue m]
     (delete (url-with-path (format "/api/queues/%s/%s/contents" (URLEncoder/encode vhost) (URLEncoder/encode queue))) m)))

(defn get-message
  [^String vhost ^String queue]
  )

(defn list-bindings
  ([]
     (get-and-decode-json (url-with-path "/api/bindings")))
  ([^String vhost]
     (get-and-decode-json (url-with-path (format "/api/bindings/%s" (URLEncoder/encode vhost))) {}))
  ([^String vhost ^String queue]
     (list-bindings vhost queue {}))
  ([^String vhost ^String queue m]
     (get-and-decode-json (url-with-path (format "/api/queues/%s/%s/bindings"
                                                 (URLEncoder/encode vhost)
                                                 (URLEncoder/encode queue))) m)))

(defn bind
  ([^String vhost ^String exchange ^String queue]
     (bind vhost exchange queue {} {}))
  ([^String vhost ^String exchange ^String queue properties]
     (bind vhost exchange queue properties {}))
  ([^String vhost ^String exchange ^String queue properties m]
     (post (url-with-path (format "/api/bindings/%s/e/%s/q/%s"
                                  (URLEncoder/encode vhost)
                                  (URLEncoder/encode exchange)
                                  (URLEncoder/encode queue))) (merge m {:body properties}))))

(defn list-vhosts
  ([]
     (list-vhosts {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/vhosts") m)))

(defn vhost-exists?
  ([^String vhost]
     (vhost-exists? vhost {}))
  ([^String vhost m]
     (let [{:keys [status]} (head (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost))) m)]
       (not (missing? status)))))

(defn get-vhost
  ([^String vhost]
     (get-vhost vhost {}))
  ([^String vhost m]
     (get-and-decode-json (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost))) m)))

(defn add-vhost
  ([^String vhost]
     (add-vhost vhost {}))
  ([^String vhost m]
     (put (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost))) (merge m {:body {:name vhost}}))))

(defn ^{:deprecated true} declare-vhost
  "Deprecated. Use add-vhost."
  [^String vhost]
  (add-vhost vhost))

(defn delete-vhost
  ([^String vhost]
     (delete-vhost vhost {}))
  ([^String vhost m]
     (delete (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost))) m)))

(defn list-permissions
  ([]
     (get-and-decode-json (url-with-path "/api/permissions")))
  ([^String vhost]
     (list-permissions vhost {}))
  ([^String vhost m]
     (get-and-decode-json (url-with-path (format "/api/vhosts/%s/permissions" (URLEncoder/encode vhost))) m)))

(defn get-permissions
  ([^String vhost ^String username]
     (get-permissions vhost username {}))
  ([^String vhost ^String username m]
     (get-and-decode-json (url-with-path (format "/api/permissions/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode username))) m)))

(defn  set-permissions
  ([^String vhost ^String username {:keys [configure write read] :as options}]
     {:pre [(every? string? [configure write read])]}
     (put (url-with-path (format "/api/permissions/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode username))) {:body options})))

(defn ^{:deprecated true} declare-permissions
  "Deprecated. Use set-permissions."
  [^String vhost ^String username {:keys [configure write read] :as body}]
  {:pre [(every? string? [configure write read])]}
  (set-permissions vhost username body))

(defn delete-permissions
  [^String vhost ^String username]
  (delete (url-with-path (format "/api/permissions/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode username)))))

(defn list-users
  ([]
     (list-users {}))
  ([m]
     (get-and-decode-json (url-with-path "/api/users") m)))

(defn get-user
  ([^String user]
     (get-user user {}))
  ([^String user m]
     (get-and-decode-json (url-with-path (format "/api/users/%s" (URLEncoder/encode user))) m)))

(defn user-exists?
  ([^String user]
     (user-exists? user {}))
  ([^String user m]
     (let [{:keys [status]} (head (url-with-path (format "/api/users/%s" (URLEncoder/encode user))) m)]
       (not (missing? status)))))

(defn add-user
  ([^String user password tags]
     (add-user user password tags {}))
  ([^String user password tags m]
     (put (url-with-path (format "/api/users/%s" (URLEncoder/encode user))) (merge m {:body {:username user :password password :tags tags :has-password true}}))))

(defn ^{:deprecated true} declare-user
  "Deprecated. Use add-user."
  [^String user password tags]
  (add-user user password tags))

(defn delete-user
  ([^String user]
     (delete-user user {}))
  ([^String user m]
     (delete (url-with-path (format "/api/users/%s" (URLEncoder/encode user))) m)))

(defn list-policies
  []
  (get-and-decode-json (url-with-path "/api/policies")))

(defn get-policies
  ([^String vhost]
     (get-and-decode-json (url-with-path (format "/api/policies/%s" (URLEncoder/encode vhost)))))
  ([^String vhost ^String name]
     (get-and-decode-json (url-with-path (format "/api/policies/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode name))))))

(defn set-policy
  [^String vhost ^String name policy]
  (put (url-with-path (format "/api/policies/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode name))) {:body policy}))

(defn ^{:deprecated true} declare-policy
  "Deprecated. Use set-policy."
  [^String vhost ^String name policy]
  (set-policy vhost name policy))

(defn delete-policy
  [^String vhost]
  (delete (url-with-path (format "/api/policies/%s" (URLEncoder/encode vhost)))))

(defn whoami
  []
  (get-and-decode-json (url-with-path "/api/whoami")))

(defn aliveness-test
  [^String vhost]
  (get-and-decode-json (url-with-path (format "/api/aliveness-test/%s" (URLEncoder/encode vhost)))))
