(ns langohr.examples.basic-consume
  "Basic-consume CLI. Run it with:

       lein exec src/langohr/examples/basic_consume.clj

  "
  (:import (java.security SecureRandom)
           (java.math.BigInteger)
           (com.rabbitmq.client Connection))
  (require [langohr.consumers :as lhcons])
  (:use [langohr.core :only [connect close]]
        [langohr.basic :only [consume]]
        [clojure.tools.cli]))

(def ^:dynamic *langohr-settings*)
(declare message-handler)

(defn consume-one
  [queue]
  (let [conn         (connect { :username (:username *langohr-settings*) :password (:password *langohr-settings*) :vhost (:vhost *langohr-settings*) })
        channel      (.createChannel conn)
        consumer-tag (.toString (new BigInteger 130 (SecureRandom.)) 32)
        consumer     (lhcons/create-default channel :handle-delivery-fn message-handler)
        ]
    (do
      (println (str "Going to consume messages from  " queue))
      (println (str "Consumer tag is " consumer-tag))
      (consume channel queue consumer :consumer-tag consumer-tag, :auto-ack true)
      (close channel)
      (close conn))))

(defn message-handler
  [delivery message-properties message-payload]
  (println (str "Got a message: " (String. message-payload) ", headers: " (str (.getHeaders message-properties)))))

(do
  []
  (let [
        args   *command-line-args*
        parsed-args (cli args
                         (optional ["--username" "Username" :default "langohr"])
                         (optional ["--password" "Password" :default "langohr"])
                         (optional ["--vhost" "RabbitMQ Virtual Host" :default "langohr.dev"])
                         (required ["--queue-name" "Queue name"])) ]

  (consume-one (:queue-name parsed-args))))
