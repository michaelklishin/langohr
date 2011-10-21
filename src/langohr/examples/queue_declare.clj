(ns langohr.examples.queue-declare
  "Queue declare CLI. Run it with:

       lein exec src/langohr/examples/queue_declare.clj

  "
  (:import (com.rabbitmq.client Channel Connection AMQP$Queue$DeclareOk))
  (:use [langohr.core :only [connect close]]
        [langohr.queue :as lhc]
        [clojure.tools.cli]))


(def ^:dynamic *langohr-settings*)

(defn client-named
  [queue-name]
  (let [conn       (connect { :username (:username *langohr-settings*) :password (:password *langohr-settings*) :vhost (:vhost *langohr-settings*) })
        channel    (.createChannel conn)
        declare-ok (lhc/declare channel queue-name :durable true :exclusive false :auto-delete false)]
    (do
      (println (str "Going to declare a queue named " queue-name))
      (println (str "Declared: " (.getQueue declare-ok)))
      (println (str "Messages count: " (.getMessageCount declare-ok)))
      (close channel)
      (close conn)
      )))


(defn server-named
  []
  (let [conn        (connect { :username "langohr", :password "langohr", :vhost "langohr.dev" })
        channel     (.createChannel conn)
        declare-ok  (lhc/declare channel)]
    (do
      (println "Going to declare a server-named queue")
      (println (str "Declared: " (.getQueue declare-ok)))
      (close channel)
      (close conn))))

(do
  (let [
        args        *command-line-args*
        parsed-args (cli args
                         (optional ["--username" "Username" :default "langohr"])
                         (optional ["--password" "Password" :default "langohr"])
                         (optional ["--vhost" "RabbitMQ Virtual Host" :default "langohr.dev"])
                         (optional ["--queue-name" "Queue name"])) ]
    (def ^:dynamic *langohr-settings* parsed-args)
    (let [queue-name (:queue-name parsed-args) ]
      (if (nil? queue-name)
        (server-named)
        (client-named queue-name)))))
