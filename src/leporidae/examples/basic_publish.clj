(ns leporidae.examples.basic-publish
  (:use [leporidae.core :only [connect close]] [leporidae.basic :only [publish]]))

(defn default-attributes
  [& arguments]
  (let [conn         (connect)
        ch           (.createChannel conn)
        payload      (or (first arguments) "Clj rcks")
        routing-key  (or (second arguments)  "amqpgem.examples.hello_world")
        exchange     ""]
    (do
      (println (str "Going to publish " payload))
      (leporidae.basic/publish ch payload { :routing-key routing-key, :exchange exchange })
      (close ch)
      (close conn))))


(defn with-type-and-priority
  [& arguments]
  (let [conn         (leporidae.core/connect)
        ch           (.createChannel conn)
        payload      (or (first arguments) "Clj rcks")
        routing-key  (or (second arguments)  "amqpgem.examples.hello_world")
        exchange     ""]
    (do
      (println (str "Going to publish " payload))
      (leporidae.basic/publish ch payload {:exchange exchange, :routing-key routing-key, :type "simple.message", :priority 7, :persistent true })
      (leporidae.core/close ch)
      (leporidae.core/close conn))))
