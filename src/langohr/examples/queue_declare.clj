(ns langohr.examples.queue-declare
  (:import (com.rabbitmq.client Channel))
  (:use [langohr.core :only [connect close]])
  (:require langohr.queue))

(defn client-named
  [name]
  (let [conn       (connect { :username "langohr", :password "langohr", :vhost "langohr.dev" })
        ch         (.createChannel conn)
        declare-ok (langohr.queue/declare ch name { :durable true, :auto-delete false })]
    (do
      (println (str "Going to declare a queue named " name))
      (println (str "Declared, messages count: " (.getMessageCount declare-ok)))
      ;;(close ch)
      ;;(close conn)
      )))


(defn server-named
  []
  (let [conn        (connect { :username "langohr", :password "langohr", :vhost "langohr.dev" })
        ch          (.createChannel conn)
        declare-ok  (langohr.queue/declare ch)]
    (do
      (println "Going to declare a server-named queue")
      (println (str "Declared: " (.getQueue declare-ok)))
      (close ch)
      (close conn))))
