;; Copyright (c) 2011-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.http
  "RabbitMQ HTTP API client.

   Relevant documentation guides:

   * http://www.rabbitmq.com/management.html
   * http://hg.rabbitmq.com/rabbitmq-management/raw-file/rabbitmq_v3_2_4/priv/www/api/index.html"
  (:refer-clojure :exclude [get])
  (:require [clj-http.client :as http]
            [cheshire.core   :as json]
            [clojure.string  :as s])
  (:import java.net.URLEncoder))

;;
;; Implementation
;;

;; a good default for now. RabbitMQ 3.0 will redirect
;; from port 55672 to 15672.
(def ^:dynamic *endpoint* "http://127.0.0.1:15672")

(def ^:dynamic *username* "guest")
(def ^:dynamic *password* "guest")


;;
;; Implementation
;;

(def ^:const throw-exceptions false)

(def ^{:const true} slash    "/")

(defn url-with-path
  [& segments]
  (str *endpoint* slash (s/join slash segments)))


(defn safe-json-decode
  "Try to parse json response. If the content-type is not json, just return the body (string)."
  [{body :body {content-type "content-type"} :headers}]
  (if (.contains (.toLowerCase ^String content-type) "json")
    (json/decode body true)
    body))

(defn ^{:private true} post
  [^String uri {:keys [body] :as options}]
  (io! (:body (http/post uri (merge options {:accept :json :basic-auth [*username* *password*] :body (json/encode body) :content-type "application/json"}))) true))

(defn ^{:private true} put
  [^String uri {:keys [body] :as options}]
  (io! (:body (http/put uri (merge options {:accept :json :basic-auth [*username* *password*] :body (json/encode body) :throw-exceptions throw-exceptions :content-type "application/json"}))) true))

(defn ^{:private true} get
  ([^String uri]
     (io! (http/get uri {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions :content-type "application/json"})))
  ([^String uri options]
     (io! (http/get uri (merge options {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions :content-type "application/json"})))))

(defn ^{:private true} get-and-decode-json
  ([^String uri]
     (safe-json-decode (get uri)))
  ([^String uri options]
     (safe-json-decode (get uri options))))

(defn ^{:private true} head
  [^String uri]
  (io! (http/head uri {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions})))

(defn ^{:private true} delete
  ([^String uri]
     (io! (:body (http/delete uri {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions})) true))
  ([^String uri &{:keys [body] :as options}]
     (io! (:body (http/delete uri (merge options {:accept :json :basic-auth [*username* *password*] :body (json/encode body) :throw-exceptions throw-exceptions}))) true)))

(defn ^{:private true} missing?
  [status]
  (= status 404))

;;
;; API
;;

(defn connect!
  [^String endpoint ^String username ^String password]
  (alter-var-root (var *endpoint*) (constantly endpoint))
  (alter-var-root (var *username*) (constantly username))
  (alter-var-root (var *password*) (constantly password)))


(defn get-overview
  []
  (get-and-decode-json (url-with-path "/api/overview")))


(defn list-nodes
  []
  (get-and-decode-json (url-with-path "/api/nodes")))

(defn get-node
  [^String node]
  (get-and-decode-json (url-with-path (str "/api/nodes/" node))))


(defn list-extensions
  []
  (get-and-decode-json (url-with-path "/api/extensions")))


(defn list-definitions
  []
  (get-and-decode-json (url-with-path "/api/definitions")))

(defn list-connections
  []
  (get-and-decode-json (url-with-path "/api/connections")))

(defn get-connection
  [^String id]
  (get-and-decode-json (url-with-path (str "/api/connections/" id))))

(defn close-connection
  [^String id]
  (delete (url-with-path (str "/api/connections/" id))))

(defn list-channels
  []
  (get-and-decode-json (url-with-path "/api/channels")))

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
  [^String vhost ^String exchange]
  (delete (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

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
     (get-and-decode-json (url-with-path (format "/api/queues/%s" (URLEncoder/encode vhost))))))

(defn get-queue
  [^String vhost ^String queue]
  (get-and-decode-json (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue)))))

(defn declare-queue
  [^String vhost ^String queue properties]
  (put (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue))) {:body properties}))

(defn delete-queue
  [^String vhost ^String queue]
  (delete (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue)))))

(defn purge-queue
  [^String vhost ^String queue]
  (delete (url-with-path (format "/api/queues/%s/%s/contents" (URLEncoder/encode vhost) (URLEncoder/encode queue)))))

(defn get-message
  [^String vhost ^String queue]
  )

(defn list-bindings
  ([]
     (get-and-decode-json (url-with-path "/api/bindings")))
  ([^String vhost]
     (get-and-decode-json (url-with-path (format "/api/bindings/%s" (URLEncoder/encode vhost)))))
  ([^String vhost ^String queue]
     (get-and-decode-json (url-with-path (format "/api/queues/%s/%s/bindings"
                                 (URLEncoder/encode vhost)
                                 (URLEncoder/encode queue))))))

(defn bind
  ([^String vhost ^String exchange ^String queue]
     (bind vhost exchange queue {}))
  ([^String vhost ^String exchange ^String queue properties]
     (post (url-with-path (format "/api/bindings/%s/e/%s/q/%s"
                                  (URLEncoder/encode vhost)
                                  (URLEncoder/encode exchange)
                                  (URLEncoder/encode queue))) {:body properties})))

(defn list-vhosts
  []
  (get-and-decode-json (url-with-path "/api/vhosts")))

(defn vhost-exists?
  [^String vhost]
  (let [{:keys [status]} (head (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost))))]
    (not (missing? status))))

(defn get-vhost
  [^String vhost]
  (get-and-decode-json (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost)))))

(defn declare-vhost
  [^String vhost]
  (put (url-with-path (format "api/vhosts/%s" (URLEncoder/encode vhost))) {:body {:name vhost}}))

(defn delete-vhost
  [^String vhost]
  (delete (url-with-path (format "/api/vhosts/%s" (URLEncoder/encode vhost)))))

(defn list-permissions
  ([]
    (get-and-decode-json (url-with-path "/api/permissions")))
  ([^String vhost]
    (get-and-decode-json (url-with-path (format "/api/vhosts/%s/permissions" (URLEncoder/encode vhost))))))

(defn get-permissions
  [^String vhost ^String username]
  (get-and-decode-json (url-with-path (format "/api/permissions/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode username)))))

(defn declare-permissions
  [^String vhost ^String username {:keys [configure write read] :as body}]
  {:pre [(every? string? [configure write read])]}
  (put (url-with-path (format "/api/permissions/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode username))) {:body body}))

(defn delete-permissions
  [^String vhost ^String username]
  (delete (url-with-path (format "/api/permissions/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode username)))))

(defn list-users
  []
  (get-and-decode-json (url-with-path "/api/users")))

(defn get-user
  [^String user]
  (get-and-decode-json (url-with-path (format "/api/users/%s" (URLEncoder/encode user)))))

(defn user-exists?
  [^String user]
  (let [{:keys [status]} (head (url-with-path (format "/api/users/%s" (URLEncoder/encode user))))]
    (not (missing? status))))

(defn add-user
  [^String user password tags]
  (put (url-with-path (format "/api/users/%s" (URLEncoder/encode user))) {:body {:username user :password password :tags tags :has-password true}}))

(defn ^{:deprecated true} declare-user
  "Deprecated. Use add-user."
  [^String user password tags]
  (add-user user password tags))

(defn delete-user
  [^String user]
  (delete (url-with-path (format "/api/users/%s" (URLEncoder/encode user)))))

(defn list-policies
  []
  (get-and-decode-json (url-with-path "/api/policies")))

(defn get-policies
  ([^String vhost]
    (get-and-decode-json (url-with-path (format "/api/policies/%s" (URLEncoder/encode vhost)))))
  ([^String vhost ^String name]
  (get-and-decode-json (url-with-path (format "/api/policies/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode name))))))

(defn declare-policy
  [^String vhost ^String name policy]
  (put (url-with-path (format "/api/policies/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode name))) {:body policy}))

(defn delete-policy
  [^String vhost]
  (delete (url-with-path (format "/api/policies/%s" (URLEncoder/encode vhost)))))

(defn whoami
  []
  (get-and-decode-json (url-with-path "/api/whoami")))

(defn aliveness-test
  [^String vhost]
  (get-and-decode-json (url-with-path (format "/api/aliveness-test/%s" (URLEncoder/encode vhost)))))
