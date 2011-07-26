(ns langohr.examples.queue-bind
  (:import (com.rabbitmq.client Channel))
  (:use [langohr.core :only [connect close]])
  (:require [langohr.queue]))

(defn to-amq-fanout
  []
  (let [conn          (connect  { :username "langohr", :password "langohr", :vhost "langohr.dev" })
         ch            (.createChannel conn)
         declare-ok    (langohr.queue/declare ch)
         queue-name    (.getQueue declare-ok)
         exchange-name "amq.fanout"]
   (do
     (println (str "Going to bind a queue named " queue-name " to " exchange-name))
     (langohr.queue/bind ch queue-name exchange-name)
     ;;(close ch)
     ;;(close conn)
     )))
