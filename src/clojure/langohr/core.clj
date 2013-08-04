;; Copyright (c) 2011-2013 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.core
  (:import [com.rabbitmq.client Connection Channel Address ConnectionFactory ShutdownListener])
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
  version "1.3.0")

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


(defn shutdown-listener
  "Adds new shutdown signal listener that delegates to given function"
  [^clojure.lang.IFn f]
  (reify ShutdownListener
    (shutdownCompleted [this cause]
      (f cause))))


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
;; Implementation
;;

(defn normalize-settings
  "For setting maps that contain keys such as :host, :username, :vhost, returns the argument"
  [config]
  (merge (settings-from (:uri config (System/getenv "RABBITMQ_URL")))
         config))

(def ^{:private true}
  client-properties {"product"      "Langohr"
                     "information"  "See http://clojurerabbitmq.info/"
                     "platform"     "Java"
                     "capabilities" {"exchange_exchange_bindings" true
                                     "consumer_cancel_notify" true
                                     "basic.nack" true
                                     "publisher_confirms" true}
                     "copyright" "Copyright (C) 2011-2013 Michael S. Klishin, Alex Petrov"
                     "version"   version})

(defn- ^ConnectionFactory create-connection-factory
  "Creates connection factory from given attributes"
  [settings]
  (let [{:keys [host port username password vhost
                requested-heartbeat connection-timeout ssl ssl-context socket-factory sasl-config]
         :or {requested-heartbeat ConnectionFactory/DEFAULT_HEARTBEAT
              connection-timeout  ConnectionFactory/DEFAULT_CONNECTION_TIMEOUT}} (normalize-settings settings)
              cf   (ConnectionFactory.)
              port' (if (and ssl (= port ConnectionFactory/DEFAULT_AMQP_PORT))
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
      (.setPort               port')
      (.setRequestedHeartbeat requested-heartbeat)
      (.setConnectionTimeout  connection-timeout))
    (when sasl-config
      (.setSaslConfig cf sasl-config))
    (when ssl-context
      (.useSslProtocol cf ^javax.net.ssl.SSLContext ssl-context))
    cf))
