(ns leporidae.examples.basic-consume
  (:import (java.security SecureRandom) (java.math.BigInteger))
  (:use [leporidae.core :only [connect close]] [leporidae.basic :only [consume]]))

(declare message-handler)

(defn default
  [queue]
  (let [conn         (connect)
        ch           (.createChannel conn)
        consumer-tag (.toString (new BigInteger 130 (SecureRandom.)) 32)]
    (do
      (println (str "Going to consume messages from  " queue))
      (println (str "Consumer tag is " consumer-tag))
      (consume ch queue message-handler { :consumer-tag consumer-tag, :auto-ack true })
      (close ch)
      (close conn))))

(defn message-handler
  [delivery message-properties message-payload]
  (println (str "Got a message: " (String. message-payload) ", headers: " (str (.getHeaders message-properties)))))