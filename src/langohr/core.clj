;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.core
  (:import (com.rabbitmq.client ConnectionFactory Connection Channel))
  (:require [langohr.channel]))

;;
;; Defaults
;;

(def ^:dynamic *default-username* "guest")
(def ^:dynamic *default-password* "guest")
(def ^:dynamic *default-vhost*    "/")
(def ^:dynamic *default-host*     "localhost")
(def ^:dynamic *default-port*     5672)


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
(defn connect
  "Creates and returns a new connection to RabbitMQ"
  ;; defaults
  (^Connection []
               (let [conn-factory (ConnectionFactory.)]
                 (.newConnection conn-factory)))
  ;; settings
  (^Connection [settings]
               (let [^ConnectionFactory conn-factory (create-connection-factory settings)]
                 (.newConnection conn-factory))))


(defn create-channel
  "Delegates to langohr.channel/open, kept for backwards compatibility"
  ^Channel [& args]
  (apply langohr.channel/open args))



;;
;; Implementation
;;

(defn create-connection-factory
  "Creates connection factory from given attributes"
  ^ConnectionFactory [{ :keys [host port username password vhost] :or {username *default-username*, password *default-password*, vhost *default-vhost*, host *default-host*, port *default-port* }}]
  (doto (ConnectionFactory.)
    (.setUsername    username)
    (.setPassword    password)
    (.setVirtualHost vhost)
    (.setHost        host)
    (.setPort        port)))