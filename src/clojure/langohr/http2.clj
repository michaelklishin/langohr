(ns langohr.http2
  (:require [cheshire.core :as json]
            [hato.client :as hato.client]
            [clojure.string :as str])
  (:import [java.util.concurrent Executors ThreadFactory]
           [java.util.concurrent.atomic AtomicLong]
           [java.net URLEncoder]))

(defonce DEFAULT-CLIENT
  (delay
    (hato.client/build-http-client
     {:connect-timeout 5000
      :redirect-policy :never
      :version :http-2 
      :executor (try
                  (eval ;; test Virtual thread availability java 19+
                   `(-> (Thread/ofVirtual)
                        (.name "langohr.http-" 0)
                        (.factory)
                        (java.util.concurrent.Executors/newThreadPerTaskExecutor)))
                  (catch Exception _
                    ;; fallback to standard utilities
                    (let [counter (AtomicLong. 0)]
                      (Executors/newCachedThreadPool
                       (reify ThreadFactory
                         (newThread [_ runnable]
                           (doto (Thread. runnable)
                             (.setName (str "langohr.http-" (.getAndIncrement counter))))))))))})))


(defonce GLOBAL-SERVER
  (atom
   {:endpoint "http://localhost:15672"
    :username "guest"
    :password "guest"}))

(defn- set-server!
  [host-opts]
  (swap! GLOBAL-SERVER merge host-opts))

(defmacro with-tmp-global-host
  [host-opts & body]
  `(let [[previous# current#] (swap-vals! GLOBAL-SERVER merge ~host-opts)]
     (try ~@body
          (finally
            (reset! GLOBAL-SERVER previous#)))))

(defonce default-http-opts
  {:accept :json
   :content-type "application/json"
   :throw-exceptions? false})

(defn- basic-auth*  [user password] {:basic-auth {:user user :pass password}})
(defn- with-client* [c] (or c @DEFAULT-CLIENT))

(defn GET
  ([uri]
   (GET uri {}))
  ([uri options]
   (io!
    (->> (update options :http-client with-client*)
         (merge default-http-opts)
         (hato.client/get uri)))))

(defn HEAD
  ([uri]
   (HEAD uri {}))
  ([uri options]
   (io!
    (->> (update options :http-client with-client*)
         (merge default-http-opts)
         (hato.client/head uri)))))

(defn POST
  ([uri]
   (POST uri {}))
  ([uri options]
   (io!
    (->> (-> options
             (update :body json/generate-string)
             (update :http-client with-client*))
         (merge default-http-opts)
         (hato.client/post uri))
    true)))

(defn PUT
  ([uri]
   (PUT uri {}))
  ([uri options]
   (io!
    (->> (-> options
             (update :body json/generate-string)
             (update :http-client with-client*))
         (merge default-http-opts)
         (hato.client/put uri))
    true)))

(defn DELETE
  ([uri]
   (DELETE uri {}))
  ([uri options]
   (io!
    (->> (-> options
             (update :body json/generate-string)
             (update :http-client with-client*))
         (merge default-http-opts)
         (hato.client/delete uri))
    true)))

(defn- not-found?
  [status]
  (= status 404))

(defn- normalize-protocol
  [proto]
  (if (= proto "amqp/ssl")
    "amqps"
    (str/lower-case proto)))

(defn- maybe-parse-json
  "Try to parse json response. If the content-type is not json, just return the body (string)."
  [{body :body {content-type "content-type"} :headers}]
  (when (some-> body seq)
    (if (re-find #"(?i)json" content-type)
      (json/parse-string body true)
      body)))

(defn- get-json*
  ([uri]
   (get-json* uri {}))
  ([uri options]
   (maybe-parse-json (GET uri options))))

(defn- url-encode
  ^String [^String fragment]
  (URLEncoder/encode fragment))

(defn- optional-url-fragment
  [fragment]
  (some->> fragment url-encode (str \/)))

;; =====
;; API
;; =====

(defn connect!
  "Mutates the global server details.
   Allows for passing less arguments (often 0) 
   in the functions below, but is generally not recommended.
   For full visibility, prefer the arities taking a <server> 
   as the first arg, and <opts> as the last arg. 
   This way you never have to worry about shared mutation."
  [endpoint username password]
  (set-server!
   {:endpoint endpoint
    :username username
    :password password}))

(defn get-overview
  ([]
   (get-overview @GLOBAL-SERVER))
  ([server]
   (get-overview server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/overview")
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-enabled-protocols
  ([]
   (list-enabled-protocols @GLOBAL-SERVER))
  ([server]
   (list-enabled-protocols server {}))
  ([server opts]
   (->> (get-overview server opts)
        :listeners
        (into #{} (map :protocol)))))

(defn list-nodes
  ([]
   (list-nodes @GLOBAL-SERVER))
  ([server]
   (list-nodes server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/nodes")
       (get-json* (merge opts (basic-auth*  username password))))))

(defn protocol-ports
  ([]
   (protocol-ports @GLOBAL-SERVER))
  ([server]
   (protocol-ports server {}))
  ([server opts]
   (->> (get-overview server opts)
        :listeners
        (into {} (map (juxt (comp normalize-protocol :protocol) :port))))))

(defn get-node
  ([node]
   (get-node @GLOBAL-SERVER node))
  ([server node]
   (get-node server node {}))
  ([{:keys [endpoint username password]} node opts]
   (-> (str endpoint "/api/nodes/" node)
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-extensions
  ([]
   (list-extensions @GLOBAL-SERVER))
  ([server]
   (list-extensions server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/extensions")
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-definitions
  ([]
   (list-definitions @GLOBAL-SERVER))
  ([server]
   (list-definitions server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/definitions")
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-connections
  ([]
   (list-connections @GLOBAL-SERVER))
  ([server]
   (list-connections server {}))
  ([{:keys [endpoint  username password]} opts]
   (-> (str endpoint "/api/connections")
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-connections-from
  ([user]
   (list-connections-from @GLOBAL-SERVER user))
  ([server user]
   (list-connections-from server user {}))
  ([server user opts]
   (->> (list-connections server opts)
        (filter (comp (partial = user) :user)))))

(defn get-connection
  ([id]
   (get-connection @GLOBAL-SERVER id))
  ([server id]
   (get-connection server id {}))
  ([{:keys [endpoint username password]} id opts]
   (-> (str endpoint "/api/connections/" id)
       (get-json* (merge opts (basic-auth* username password))))))

(defn close-connection
  ([^String id]
   (close-connection @GLOBAL-SERVER id))
  ([server id]
   (close-connection server id {}))
  ([{:keys [endpoint username password]} id opts]
   (-> (str endpoint "/api/connections/" id)
       (DELETE (merge opts (basic-auth* username password))))))

(defn close-all-connections
  ([]
   (close-all-connections @GLOBAL-SERVER))
  ([server]
   (->> (list-connections server)
        (map :name)
        (run! (partial close-connection server)))))

(defn close-connections-from
  ([user]
   (close-connections-from @GLOBAL-SERVER user))
  ([server user]
   (close-connections-from server user {}))
  ([server user opts]
   (->> (list-connections-from server user opts)
        (map :name)
        (run! #(close-connection server % opts)))))

(defn list-channels
  ([]
   (list-channels @GLOBAL-SERVER))
  ([server]
   (list-channels server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/channels")
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-exchanges 
  ([]
   (list-exchanges nil))
  ([vhost]
   (list-exchanges @GLOBAL-SERVER vhost))
  ([server vhost]
   (list-exchanges (or server @GLOBAL-SERVER) vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/exchanges" (optional-url-fragment vhost))
       (get-json* (merge opts (basic-auth* username password))))))

(defn get-exchange
  ([vhost exchange]
   (get-exchange @GLOBAL-SERVER vhost exchange))
  ([server vhost exchange]
   (get-exchange server vhost exchange {}))
  ([{:keys [endpoint username password]} vhost exchange opts]
   (-> (str endpoint "/api/exchanges/" (url-encode vhost) \/ (url-encode exchange))
       (get-json* (merge opts (basic-auth* username password))))))

(defn declare-exchange
  ([vhost exchange properties]
   (declare-exchange @GLOBAL-SERVER vhost exchange properties))
  ([server vhost exchange properties]
   (declare-exchange server vhost exchange properties {}))
  ([{:keys [endpoint username password]} ^String vhost ^String exchange properties opts]
   (-> (str endpoint "/api/exchanges/" (url-encode vhost) \/ (url-encode exchange))
       (PUT (-> opts
                (assoc :body properties)
                (merge (basic-auth* username password)))))))

(defn delete-exchange
  ([vhost exchange]
   (delete-exchange @GLOBAL-SERVER vhost exchange))
  ([host vhost exchange]
   (delete-exchange host vhost exchange {}))
  ([{:keys [endpoint username password]} vhost exchange opts]
   (-> (str endpoint "/api/exchanges/" (url-encode vhost) \/ (url-encode exchange))
       (DELETE (merge opts (basic-auth* username password))))))

(defn list-bindings-for-which-exchange-is-the-source
  ([vhost exchange]
   (list-bindings-for-which-exchange-is-the-source @GLOBAL-SERVER vhost exchange))
  ([server vhost exchange]
   (list-bindings-for-which-exchange-is-the-source server vhost exchange {}))
  ([{:keys [endpoint username password]} vhost exchange opts]
   (-> "%s/api/exchanges/%s/%s/bindings/source"
       (format endpoint (url-encode vhost) (url-encode exchange))
       (get-json* (merge opts (basic-auth* username password))))))

(defn list-bindings-for-which-exchange-is-the-destination
  ([vhost exchange]
   (list-bindings-for-which-exchange-is-the-destination @GLOBAL-SERVER vhost exchange))
  ([server vhost exchange]
   (list-bindings-for-which-exchange-is-the-destination server vhost exchange {}))
  ([{:keys [endpoint username password]} vhost exchange opts]
   (-> "%s/api/exchanges/%s/%s/bindings/destination"
       (format endpoint (url-encode vhost) (url-encode exchange))
       (get-json* (merge opts (basic-auth* username password))))))

(defn publish
  [vhost exchange]
  ;; FIXME: ???
  )

(defn list-vhosts
  ([]
   (list-vhosts @GLOBAL-SERVER))
  ([server]
   (list-vhosts server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/vhosts")
       (get-json* (merge opts (basic-auth* username password))))))

(defn vhost-exists?
  ([^String vhost]
   (vhost-exists? @GLOBAL-SERVER vhost))
  ([server vhost]
   (vhost-exists? server vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/vhosts/" (url-encode vhost))
       (HEAD (merge opts (basic-auth* username password)))
       :status
       not-found?
       false?)))

(defn get-vhost
  ([vhost]
   (get-vhost @GLOBAL-SERVER vhost))
  ([server vhost]
   (get-vhost server vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/vhosts/" (url-encode vhost))
       (get-json* (merge opts (basic-auth* username password))))))

(defn add-vhost
  ([vhost]
   (add-vhost @GLOBAL-SERVER vhost))
  ([server vhost]
   (add-vhost server vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/vhosts/" (url-encode vhost))
       (PUT (-> opts
                (assoc-in [:body :name] vhost)
                (merge (basic-auth* username password)))))))

(defn delete-vhost
  ([vhost]
   (delete-vhost @GLOBAL-SERVER vhost))
  ([server vhost]
   (delete-vhost server vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/vhosts/" (url-encode vhost))
       (DELETE (merge opts (basic-auth* username password))))))

(defn list-permissions
  ([]
   (list-permissions @GLOBAL-SERVER))
  ([server]
   (list-permissions server nil))
  ([server vhost]
   (list-permissions server vhost {}))
  ([server vhost opts]
   (let [{:keys [endpoint username password]} (or server @GLOBAL-SERVER)
         url (if (nil? vhost) 
               (str endpoint "/api/permissions")
               (format "%s/api/vhosts/%s/permissions" endpoint (url-encode vhost)))]
     (get-json* url (merge opts (basic-auth* username password))))))

(defn get-permissions
  ([vhost user] 
   (get-permissions @GLOBAL-SERVER vhost user))
  ([server vhost user]
   (get-permissions server vhost user {}))
  ([{:keys [endpoint username password]} vhost user opts]
   (-> (str endpoint "/api/permissions/" (url-encode vhost) \/ (url-encode user))
       (get-json* (merge opts (basic-auth* username password))))))

(defn  set-permissions
  ([vhost user perms]
   (set-permissions @GLOBAL-SERVER vhost user perms))
  ([server vhost user perms] 
   (set-permissions server vhost user perms {}))
  ([{:keys [endpoint username password]} vhost user {:keys [configure write read] :as perms} opts]
   {:pre [(every? string? [configure write read])]}
   (-> (str endpoint "/api/permissions/" (url-encode vhost) \/ (url-encode user))
       (PUT (-> opts 
                (assoc :body perms)
                (merge (basic-auth* username password)))))))

(defn delete-permissions
  ([vhost user] 
   (delete-permissions @GLOBAL-SERVER vhost user))
  ([server vhost user] 
   (delete-permissions server vhost user {}))
  ([{:keys [endpoint username password]} vhost user opts]
   (-> (str endpoint "/api/permissions/" (url-encode vhost) \/ (url-encode user))
       (DELETE (merge opts (basic-auth* username password))))))

(defn list-queues
  ([] 
   (list-queues nil))
  ([vhost]
   (list-queues @GLOBAL-SERVER vhost))
  ([server vhost]
   (list-queues (or server @GLOBAL-SERVER) vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/queues" (optional-url-fragment vhost))
       (get-json* (merge opts (basic-auth* username password))))))

(defn get-queue
  ([vhost queue]
   (get-queue @GLOBAL-SERVER vhost queue))
  ([server vhost queue]
   (get-queue server vhost queue {}))
  ([{:keys [endpoint username password]} vhost queue opts]
   (-> (str endpoint "/api/queues/" (url-encode vhost) \/ (url-encode queue))
       (get-json* (merge opts (basic-auth* username password))))))

(defn declare-queue
  ([vhost queue properties]
   (declare-queue @GLOBAL-SERVER vhost queue properties))
  ([server vhost queue properties]
   (declare-queue server vhost queue properties {}))
  ([{:keys [endpoint username password]} vhost queue properties opts]
   (-> (str endpoint "/api/queues/" (url-encode vhost) \/ (url-encode queue))
       (PUT (-> opts
                (assoc :body properties)
                (merge (basic-auth* username password)))))))

(defn delete-queue
  ([vhost queue]
   (delete-queue @GLOBAL-SERVER vhost queue))
  ([server vhost queue]
   (delete-queue server vhost queue {}))
  ([{:keys [endpoint username password]} vhost queue opts]
   (-> (str endpoint "/api/queues/" (url-encode vhost) \/ (url-encode queue))
       (DELETE (merge opts (basic-auth* username password))))))

(defn purge-queue
  ([vhost queue]
   (purge-queue @GLOBAL-SERVER vhost queue))
  ([server vhost queue]
   (purge-queue server vhost queue {}))
  ([{:keys [endpoint username password]} vhost queue opts]
   (-> "%s/api/queues/%s/%s/contents"
       (format endpoint (url-encode vhost) (url-encode queue))
       (DELETE (merge opts (basic-auth* username password))))))

(defn get-message
  [^String vhost ^String queue]
  ;; FIXME: ???
  )

;; first two arities call "/api/bindings"
;; next two call  "/api/bindings/:vhost" 
;; last one calls "/api/queues/:vhost/:queue/bindings" 
(defn list-bindings
  ([]
   (list-bindings {}))
  ([opts]
   (list-bindings nil opts))
  ([vhost opts]
   (list-bindings @GLOBAL-SERVER vhost opts))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/bindings" (optional-url-fragment vhost))
       (get-json* (merge opts (basic-auth* username password)))))
  ([server vhost queue opts]
   ;; this last arity is kind of special, in that there are no global defaults
  ;; nil(s) must be explicitely passed in for defaults to fire
   (let [{:keys [endpoint username password]} (or server @GLOBAL-SERVER)]
     (-> "%s/api/queues/%s/%s/bindings"
         (format endpoint (url-encode vhost) (url-encode queue))
         (get-json* (merge opts (basic-auth* username password)))))))

(defn bind
  ([vhost exchange queue]
   (bind @GLOBAL-SERVER vhost exchange queue))
  ([server vhost exchange queue]
   (bind server vhost exchange queue {}))
  ([server vhost  exchange  queue properties]
   (bind server vhost exchange queue properties {}))
  ([{:keys [endpoint username password]} vhost  exchange queue properties opts]
   (-> "%s/api/bindings/%s/e/%s/q/%s"
       (format endpoint (url-encode vhost) (url-encode exchange) (url-encode queue))
       (POST (-> opts
                 (assoc :body properties)
                 (merge (basic-auth* username password)))))))

(defn list-users
  ([]
   (list-users @GLOBAL-SERVER))
  ([server] 
   (list-users server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/users")
       (get-json* (merge opts (basic-auth* username password))))))

(defn get-user
  ([user] 
   (get-user @GLOBAL-SERVER user))
  ([server user]
   (get-user server user {}))
  ([{:keys [endpoint username password]} user opts]
   (-> (str endpoint "/api/users/" (url-encode user))
       (get-json* (merge opts (basic-auth* username password))))))

(defn user-exists?
  ([user] 
   (user-exists? @GLOBAL-SERVER user))
  ([server user]
   (user-exists? server user {}))
  ([{:keys [endpoint username password]} user opts]
   (-> (str endpoint "/api/users/" (url-encode user))
       (HEAD (merge opts (basic-auth* username password)))
       :status 
       not-found?
       false?)))

(defn add-user
  ([user pass tags]
   (add-user @GLOBAL-SERVER user pass tags))
  ([server user pass tags]
   (add-user server user pass tags {}))
  ([{:keys [endpoint username password]} user pass tags opts]
   (->  (str endpoint "/api/users/" (url-encode user))
        (PUT (-> opts 
                 (assoc :body {:username user 
                               :password pass 
                               :tags tags 
                               :has-password true})
                 (merge (basic-auth* username password)))))))

(defn delete-user
  ([user] 
   (delete-user @GLOBAL-SERVER user))
  ([server user]
   (delete-user server user {}))
  ([{:keys [endpoint username password]} user opts]
   (-> (str endpoint "/api/users/" (url-encode user))
       (DELETE (merge opts (basic-auth* username password))))))

(defn list-policies
  ([] 
   (list-policies @GLOBAL-SERVER))
  ([server] 
   (list-policies server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/policies")
       (get-json* (merge opts (basic-auth* username password))))))

(defn get-policies
  ([vhost] 
   (get-policies @GLOBAL-SERVER vhost))
  ([server vhost]
   (get-policies server vhost nil))
  ([server vhost policy-name] 
   (get-policies server vhost policy-name {}))
  ([{:keys [endpoint username password]} vhost  policy-name opts]
   (-> (str endpoint "/api/policies/" (url-encode vhost) (optional-url-fragment policy-name))
       (get-json* (merge opts (basic-auth* username password))))))

(defn set-policy
  ([vhost policy-name policy] 
   (set-policy @GLOBAL-SERVER vhost policy-name policy))
  ([server vhost policy-name policy]
   (set-policy server vhost policy-name policy {}))
  ([{:keys [endpoint username password]} vhost policy-name policy opts]
   (->  (str endpoint "/api/policies/" (url-encode vhost) \/ (url-encode policy-name))
        (PUT (-> opts
                 (assoc :body policy)
                 (merge (basic-auth* username password)))))))

(defn delete-policy
  ([vhost] 
   (delete-policy @GLOBAL-SERVER vhost))
  ([server vhost] 
   (delete-policy server vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/policies/" (url-encode vhost))
       (DELETE (merge opts (basic-auth* username password))))))

(defn whoami
  ([] 
   (whoami @GLOBAL-SERVER))
  ([server] 
   (whoami server {}))
  ([{:keys [endpoint username password]} opts]
   (-> (str endpoint "/api/whoami")
       (get-json* (merge opts (basic-auth* username password))))))

(defn aliveness-test
  ([vhost] 
   (aliveness-test @GLOBAL-SERVER vhost))
  ([server vhost]
   (aliveness-test server vhost {}))
  ([{:keys [endpoint username password]} vhost opts]
   (-> (str endpoint "/api/aliveness-test/" (url-encode vhost))
       (get-json* (merge opts (basic-auth* username password))))))
