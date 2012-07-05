(ns langohr.examples.connection
  (:import (com.rabbitmq.client ConnectionFactory Connection Channel))
  (:use [langohr core]))

(defn default-settings-via-java-interop
  [& arguments]
  (let [conn-factory (ConnectionFactory.)
        conn         (.newConnection conn-factory)
        ch           (.createChannel conn)]
    (do
      (println "Connected.")
      (.close ch)
      (println "Closed channel...")
      (println "Disconnecting...")
      (.close conn)
      (println "Connected."))))


(defn default-settings
  [& arguments]
  (let [conn         (langohr.core/connect { :username "langohr", :password "langohr", :vhost "langohr.dev" })
        ch           (.createChannel conn)]
    (do
      (println "Connected.")
      (close ch)
      (println "Closed channel...")
      (println "Disconnecting...")
      (close conn)
      (println "Connected."))))
