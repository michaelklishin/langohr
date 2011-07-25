(ns langohr.examples.exchange-declare
  (:import (com.rabbitmq.client Channel))
  (:use [langohr.core :only [connect close]])
  (:require [langohr.exchange]))

(defn client-named
  [name type]
  (let [conn       (connect {:username "langohr", :password "langohr", :vhost "langohr.dev"})
        ch         (.createChannel conn)
        declare-ok (langohr.exchange/declare ch name type {:durable true, :auto-delete false})]
    (do
      (println (str "Declared an exchange named " name " of type " type))
      ;; (close ch)
      ;; (close conn)
      )))
