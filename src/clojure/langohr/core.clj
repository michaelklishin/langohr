;; Copyright (c) 2011-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.core
  "Functions that work with connections and shared features between connections
   and channels (e.g. shutdown listeners).

   Relevant guides:

    * http://clojurerabbitmq.info/articles/connecting.html
    * http://clojurerabbitmq.info/articles/tls.html"
  (:import [com.rabbitmq.client Connection Channel Address ConnectionFactory ShutdownListener BlockedListener]
           [com.novemberain.langohr Recoverable]
           [clojure.lang IFn]
           [com.rabbitmq.client.impl AMQConnection])
  (:require langohr.channel
            [clojure.string :as s]
            [clojure.walk   :as walk]))

;;
;; Implementation
;;

(def ^{:dynamic true :doc "Default connection settings."} *default-config*
  {:username "guest"
   :password "guest"
   :vhost     "/"
   :host      "localhost"
   :port      ConnectionFactory/DEFAULT_AMQP_PORT})

;;
;; API
;;

(defprotocol Closeable
  (close [c] "Closes given entity"))

(extend-protocol Closeable
  com.rabbitmq.client.Connection
  (close [this] (.close this))

  com.rabbitmq.client.Channel
  (close [this] (.close this)))


(defprotocol Openable
  (open? [this] "Checks whether given entity is open")
  (closed? [this] "Checks whether given entity is closed"))

(extend-protocol Openable
  com.rabbitmq.client.Connection
  (open? [conn] (.isOpen conn))
  (closed? [conn] (not (.isOpen conn)))

  com.rabbitmq.client.Channel
  (open? [ch] (.isOpen ch))
  (closed? [ch] (not (.isOpen ch))))


(def ^{:const true}
  version "2.9.0-SNAPSHOT")

(declare create-connection-factory)
(defn ^Connection connect
  "Creates and returns a new connection to RabbitMQ."
  ;; defaults
  ([]
     (let [^ConnectionFactory cf (create-connection-factory {})]
       (doto (com.novemberain.langohr.Connection. cf)
         .init)))
  ;; settings
  ([settings]
     (let [^ConnectionFactory cf (create-connection-factory settings)]
       (doto (com.novemberain.langohr.Connection. cf (dissoc settings :password :username))
         .init))))

(defn- create-address-array [addresses]
  (into-array Address
              (for [[host port] addresses]
                (Address. host (or port ConnectionFactory/DEFAULT_AMQP_PORT)))))

(defn ^Channel create-channel
  "Delegates to langohr.channel/open, kept for backwards compatibility"
  [& args]
  (apply langohr.channel/open args))


(defn ^ShutdownListener shutdown-listener
  "Adds new shutdown signal listener that delegates to given function"
  [^clojure.lang.IFn f]
  (reify ShutdownListener
    (shutdownCompleted [this cause]
      (f cause))))

(defn ^ShutdownListener add-shutdown-listener
  "Adds a shutdown listener on connection and returns it"
  [^Connection c ^IFn f]
  (let [lnr (shutdown-listener f)]
    (.addShutdownListener c lnr)
    lnr))

(defn ^BlockedListener blocked-listener
  "Reifies connection.blocked and connection.unblocked listener from Clojure
   functions"
  [^IFn on-blocked ^IFn on-unblocked]
  (reify BlockedListener
    (^void handleBlocked [this ^String reason]
      (on-blocked reason))
    (^void handleUnblocked [this]
      (on-unblocked))))

(defn ^BlockedListener add-blocked-listener
  "Adds a connection.blocked and connection.unblocked listener
   on connection and returns it"
  [^Connection c ^IFn on-blocked ^IFn on-unblocked]
  (let [lnr (blocked-listener on-blocked on-unblocked)]
    (.addBlockedListener c lnr)
    lnr))

(defn settings-from
  "Parses AMQP connection URI and returns a persistent map of settings"
  [^String uri]
  (if uri
    (let [cf (doto (ConnectionFactory.)
               (.setUri uri))]
      {:host     (.getHost cf)
       :port     (.getPort cf)
       :vhost    (.getVirtualHost cf)
       :username (.getUsername cf)
       :password (.getPassword cf)})
    *default-config*))

(defn capabilities-of
  "Returns capabilities of the broker on the other side of the connection"
  [^Connection conn]
  (walk/keywordize-keys (into {} (-> conn .getServerProperties (get "capabilities")))))

;;
;; Recovery
;;

(defn automatic-recovery-enabled?
  "Returns true if provided connection uses automatic connection recovery
   mode, false otherwise"
  [^com.novemberain.langohr.Connection conn]
  (.automaticRecoveryEnabled conn))

(defn ^{:deprecated true} automatically-recover?
  "See automatic-recovery-enabled?"
  [^com.novemberain.langohr.Connection c]
  (automatic-recovery-enabled? c))

(defn automatic-topology-recovery-enabled?
  "Returns true if provided connection uses automatic topology recovery
   mode, false otherwise"
  [^com.novemberain.langohr.Connection conn]
  (.automaticTopologyRecoveryEnabled conn))

(defn on-recovery
  "Registers a network recovery callback on a (Langohr) connection or channel"
  [^Recoverable target ^IFn callback]
  (.onRecovery target callback))


;;
;; Implementation
;;

(defn normalize-settings
  "For setting maps that contain keys such as :host, :username, :vhost, returns the argument"
  [config]
  (merge (settings-from (:uri config (System/getenv "RABBITMQ_URL")))
         config))

(defn- platform-string
  []
  (let []
    (format "Clojure %s on %s %s"
      (clojure-version)
      (System/getProperty "java.vm.name")
      (System/getProperty "java.version"))))

(def ^{:private true}
  client-properties {"product"      "Langohr"
                     "information"  "See http://clojurerabbitmq.info/"
                     "platform"     (platform-string)
                     "capabilities" (get (AMQConnection/defaultClientProperties) "capabilities")
                     "copyright" "Copyright (C) 2011-2014 Michael S. Klishin, Alex Petrov"
                     "version"   version})

(defn- ^ConnectionFactory create-connection-factory
  "Creates connection factory from given attributes"
  [settings]
  (let [{:keys [host port username password vhost
                requested-heartbeat connection-timeout ssl ssl-context socket-factory sasl-config
                requested-channel-max]
         :or {requested-heartbeat ConnectionFactory/DEFAULT_HEARTBEAT
              connection-timeout  ConnectionFactory/DEFAULT_CONNECTION_TIMEOUT
              requested-channel-max ConnectionFactory/DEFAULT_CHANNEL_MAX}} (normalize-settings settings)
              cf   (ConnectionFactory.)
              final-port (if (and ssl (= port ConnectionFactory/DEFAULT_AMQP_PORT))
                           ConnectionFactory/DEFAULT_AMQP_OVER_SSL_PORT
                           port)]
    (when (or ssl
              (= port ConnectionFactory/DEFAULT_AMQP_OVER_SSL_PORT))
      (.useSslProtocol cf))
    (doto cf
      (.setClientProperties   client-properties)
      (.setUsername           username)
      (.setPassword           password)
      (.setVirtualHost        vhost)
      (.setHost               host)
      (.setPort               final-port)
      (.setRequestedHeartbeat requested-heartbeat)
      (.setConnectionTimeout  connection-timeout)
      (.setRequestedChannelMax requested-channel-max))
    (when sasl-config
      (.setSaslConfig cf sasl-config))
    (when ssl-context
      (.useSslProtocol cf ^javax.net.ssl.SSLContext ssl-context))
    cf))

