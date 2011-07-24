(ns leporidae.examples.connection
  (:import (com.rabbitmq.client ConnectionFactory Connection Channel))
  (:use [leporidae core]))

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
  (let [conn         (leporidae.core/connect { :username "leporidae", :password "leporidae", :vhost "leporidae.dev" })
        ch           (.createChannel conn)]
    (do
      (println "Connected.")
      (close ch)
      (println "Closed channel...")
      (println "Disconnecting...")
      (close conn)
      (println "Connected."))))
