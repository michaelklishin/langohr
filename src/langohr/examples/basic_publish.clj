(ns langohr.examples.basic-publish
  (:use [langohr.core :only [connect close]] [langohr.basic :only [publish]]))

(defn default-attributes
  [& arguments]
  (let [conn         (connect)
        ch           (.createChannel conn)
        payload      (or (first arguments) "¡Hola! de Clojure! à bientôt")
        routing-key  (or (second arguments)  "langohr.examples.hello_world")
        exchange     ""]
    (do
      (println (str "Going to publish " payload))
      (publish ch exchange routing-key payload {})
      (close ch)
      (close conn))))


(defn with-type-and-priority
  [& arguments]
  (let [conn         (langohr.core/connect)
        ch           (.createChannel conn)
        payload      (or (first arguments) "¡Hola! de Clojure! à bientôt")
        routing-key  (or (second arguments)  "langohr.examples.hello_world")
        exchange     ""]
    (do
      (println (str "Going to publish " payload))
      (publish ch exchange routing-key payload { :type "simple.message", :priority 7, :persistent true })
      (close ch)
      (close conn))))
