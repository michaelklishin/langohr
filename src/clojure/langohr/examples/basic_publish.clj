(ns langohr.examples.basic-publish
  (:require [langohr.channel :as lch])
  (:use [langohr.core :only [connect close]] [langohr.basic :only [publish]]))

(defn default-attributes
  [& arguments]
  (let [conn         (connect)
        ch           (lch/open conn)
        payload      (or (first arguments) "¡Hola! de Clojure! à bientôt")
        routing-key  (or (second arguments)  "langohr.examples.hello_world")
        exchange     ""]
    (println (str "Going to publish " payload))
    (publish ch exchange routing-key payload {})
    (close ch)
    (close conn)))


(defn with-type-and-priority
  [& arguments]
  (let [conn         (langohr.core/connect)
        ch           (lch/open conn)
        payload      (or (first arguments) "¡Hola! de Clojure! à bientôt")
        routing-key  (or (second arguments)  "langohr.examples.hello_world")
        exchange     ""]
    (println (str "Going to publish " payload))
    (publish ch exchange routing-key payload { :type "simple.message", :priority 7, :persistent true })
    (close ch)
    (close conn)))
