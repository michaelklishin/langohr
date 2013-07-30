(ns langohr.http
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


(defn post
  [^String uri &{:keys [body] :as options}]
  (io! (:body (http/post uri (merge options {:accept :json :basic-auth [*username* *password*] :body (json/encode body)}))) true))

(defn put
  [^String uri &{:keys [body] :as options}]
  (io! (:body (http/put uri (merge options {:accept :json :basic-auth [*username* *password*] :body (json/encode body) :throw-exceptions throw-exceptions}))) true))

(defn get
  ([^String uri]
     (io! (json/decode (:body (http/get uri {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions})) true)))
  ([^String uri &{:as options}]
     (io! (json/decode (:body (http/get uri (merge options {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions}))) true))))

(defn head
  [^String uri]
  (io! (http/head uri {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions})))

(defn delete
  ([^String uri]
     (io! (:body (http/delete uri {:accept :json :basic-auth [*username* *password*] :throw-exceptions throw-exceptions})) true))
  ([^String uri &{:keys [body] :as options}]
     (io! (:body (http/delete uri (merge options {:accept :json :basic-auth [*username* *password*] :body (json/encode body) :throw-exceptions throw-exceptions}))) true)))



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
  (get (url-with-path "/api/overview")))


(defn list-nodes
  []
  (get (url-with-path "/api/nodes")))

(defn get-node
  [^String node]
  (get (url-with-path (str "/api/nodes/" node))))


(defn list-extensions
  []
  (get (url-with-path "/api/extensions")))


(defn list-definitions
  []
  (get (url-with-path "/api/definitions")))

(defn list-connections
  []
  (get (url-with-path "/api/connections")))

(defn get-connection
  [^String id]
  (get (url-with-path (str "/api/connections/" id))))

(defn close-connection
  [^String id]
  (delete (url-with-path (str "/api/connections/" id))))

(defn list-channels
  []
  (get (url-with-path "/api/channels")))

(defn list-exchanges
  ([]
     (get (url-with-path "/api/exchanges")))
  ([^String vhost]
     (get (url-with-path (str "/api/exchanges/" (URLEncoder/encode vhost))))))

(defn get-exchange
  [^String vhost ^String exchange]
  (get (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn declare-exchange
  [^String vhost ^String exchange properties]
  (post (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange))) :body (json/generate-string properties)))

(defn delete-exchange
  [^String vhost ^String exchange]
  (delete (url-with-path (format "/api/exchanges/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn list-bindings-for-which-exchange-is-the-source
  [^String vhost ^String exchange]
  (get (url-with-path (format "/api/exchanges/%s/%s/bindings/source" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn list-bindings-for-which-exchange-is-the-destination
  [^String vhost ^String exchange]
  (get (url-with-path (format "/api/exchanges/%s/%s/bindings/destination" (URLEncoder/encode vhost) (URLEncoder/encode exchange)))))

(defn publish
  [^String vhost ^String exchange]
  )

(defn list-queues
  ([]
     (get (url-with-path "/api/queues")))
  ([^String vhost]
     (get (url-with-path (format "/api/queues/%s" (URLEncoder/encode vhost))))))

(defn get-queue
  [^String vhost ^String queue]
  (get (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue)))))

(defn declare-queue
  [^String vhost ^String queue properties]
  (post (url-with-path (format "/api/queues/%s/%s" (URLEncoder/encode vhost) (URLEncoder/encode queue))) :body (json/generate-string properties)))

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
     (get (url-with-path "/api/bindings")))
  ([^String vhost]
     (get (url-with-path (format "/api/bindings/%s" (URLEncoder/encode vhost)))))
  ([^String vhost ^String queue]
     (get (url-with-path (format "/api/queues/%s/%s/bindings"
                                 (URLEncoder/encode vhost)
                                 (URLEncoder/encode queue))))))

(defn bind
  ([^String vhost ^String exchange ^String queue]
     (bind vhost exchange queue {}))
  ([^String vhost ^String exchange ^String queue properties]
     (post (url-with-path (format "/api/bindings/%s/e/%s/q/%s"
                                  (URLEncoder/encode vhost)
                                  (URLEncoder/encode exchange)
                                  (URLEncoder/encode queue))) :body properties)))

(defn list-vhosts
  []
  (get (url-with-path "/api/vhosts")))

(defn get-vhost
  [^String vhost]
  )

(defn list-permissions
  [^String vhost]
  )

(defn get-permissions
  [^String vhost ^String username]
  )

(defn aliveness-test
  [^String vhost]
  )
