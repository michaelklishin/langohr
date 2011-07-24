(ns leporidae.core
  (:import (com.rabbitmq.client ConnectionFactory Connection))
  )

;;
;; Defaults
;;

(def *default-username* "guest")
(def *default-password* "guest")
(def *default-vhost*    "/")
(def *default-host*     "localhost")
(def *default-port*     5672)


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


;;
;; API
;;

(defn connect
  "Creates and returns a new connection to RabbitMQ"
  ;; defaults
  ([]
     (let [conn-factory (ConnectionFactory.)]
       (.newConnection conn-factory)))
  ;; settings
  ([{username :username, password :password, vhost :vhost, host :host, port :port}]
     (let [conn-factory (doto (ConnectionFactory.)
                          (.setUsername    (or username *default-username*))
                          (.setPassword    (or password *default-password*))
                          (.setVirtualHost (or vhost *default-vhost*))
                          (.setHost        (or host *default-host*))
                          (.setPort        (or port *default-port*)))]
       (.newConnection conn-factory))))
