;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.core
  (:import (com.rabbitmq.client ConnectionFactory Connection Channel ShutdownListener))
  (:require [langohr.channel]))

;;
;; Defaults
;;

(def ^{:dynamic true :doc "Default connection settings."} *default-config*
  {:username "guest"
   :password "guest"
   :vhost     "/"
   :host      "localhost"
   :port      5672})

;;
;; Protocols
;;

(defprotocol Closeable
  (close [c] "Closes given entity"))

(extend-protocol Closeable
  com.rabbitmq.client.Connection
  (close [this] (.close this)))

(extend-protocol Closeable
  com.rabbitmq.client.Channel
  (close [this] (.close this)))


(defprotocol Openable
  (open? [this] "Checks whether given entity is still open"))

(extend-protocol Openable
  com.rabbitmq.client.Connection
  (open? [this] (.isOpen this)))

(extend-protocol Openable
  com.rabbitmq.client.Channel
  (open? [this] (.isOpen this)))


;;
;; API
;;

(declare create-connection-factory)
(defn ^Connection connect
  "Creates and returns a new connection to RabbitMQ."
  ;; defaults
  ([]
     (let [conn-factory (ConnectionFactory.)]
       (.newConnection conn-factory)))
  ;; settings
  ([settings]
     (let [^ConnectionFactory conn-factory (create-connection-factory settings)]
       (.newConnection conn-factory))))


(defn ^Channel create-channel
  "Delegates to langohr.channel/open, kept for backwards compatibility"
  [& args]
  (apply langohr.channel/open args))


(defn shutdown-listener
  "Adds new shutdown signal listener that delegates to given function"
  [^clojure.lang.IFn handler-fn]
  (reify ShutdownListener
    (shutdownCompleted [this cause]
      (handler-fn cause))))


;;
;; Implementation
;;

(defn- env-config [uri]
  (if uri
    (let [uri (java.net.URI. uri)
          [username password] (if (.getUserInfo uri)
                                (.split (.getUserInfo uri) ":"))
          uri-config {:host     (.getHost uri)
                      :port     (.getPort uri)
                      :vhost    (.getPath uri)
                      :username username
                      :password password}]
      (into *default-config* (filter val uri-config)))
    *default-config*))

(defn- get-config [config]
  (merge (env-config (:uri config (System/getenv "RABBITMQ_URL")))
         config))

(defn- ^ConnectionFactory create-connection-factory
  "Creates connection factory from given attributes"
  [config]
  (let [{:keys [host port username password vhost requested-heartbeat connection-timeout] :or {
                                                                                               requested-heartbeat ConnectionFactory/DEFAULT_HEARTBEAT
                                                                                               connection-timeout  10
                                                                                               } } (get-config config)]
    (doto (ConnectionFactory.)
      (.setUsername           username)
      (.setPassword           password)
      (.setVirtualHost        vhost)
      (.setHost               host)
      (.setPort               port)
      (.setRequestedHeartbeat requested-heartbeat)
      (.setConnectionTimeout  connection-timeout))))
