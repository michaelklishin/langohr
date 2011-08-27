(ns langohr.basic
  (:import (com.rabbitmq.client Channel AMQP AMQP$BasicProperties AMQP$BasicProperties$Builder QueueingConsumer GetResponse)))


;;
;; API
;;

(defn publish
  "Publishes a message using basic.publish AMQP method"
  [^Channel channel, ^String exchange, ^String routing-key, ^String payload,
   {:keys [content-type content-encoding headers
           persistent priority correlation-id reply-to expiration message-id
           timestamp type user-id app-id cluster-id]}]
  (let [payload-bytes      (.getBytes payload)
        properties-builder (AMQP$BasicProperties$Builder.)
        properties         (.build (doto properties-builder
                                     (.contentType     content-type)
                                     (.contentEncoding content-encoding)
                                     (.headers         headers)
                                     (.deliveryMode    (Integer/valueOf (if persistent 2 1)))
                                     (.priority        (if priority (Integer/valueOf priority) nil))
                                     (.correlationId   correlation-id)
                                     (.replyTo         reply-to)
                                     (.expiration      expiration)
                                     (.messageId       message-id)
                                     (.timestamp       timestamp)
                                     (.type            type)
                                     (.userId          user-id)
                                     (.appId           app-id)
                                     (.clusterId       cluster-id)))]
    (.basicPublish channel exchange routing-key properties payload-bytes)
    ))


(defn consume
  "Adds new consumer to a queue using basic.consume AMQP method"
  [^Channel channel, ^String queue, ^clojure.lang.IFn message-handler, { consumer-tag :consumer-tag auto-ack :auto-ack  }]
  (let [queueing-consumer (QueueingConsumer. channel)]
    (do
      (.basicConsume channel queue auto-ack queueing-consumer)
      (while true
        (try
          (let [delivery (.nextDelivery queueing-consumer)]
            (message-handler delivery (.getProperties delivery) (.getBody delivery)))
          (catch InterruptedException e
            nil))))))



(defn ^GetResponse get
  "Fetches a message from a queue using basic.get AMQP method"
  ([^Channel channel, ^String queue]
     (.basicGet channel queue true))
  ([^Channel channel, ^String queue, auto-ack]
     (.basicGet channel queue auto-ack)))



(defn qos
  "Sets channel or connection prefetch level using basic.qos AMQP method"
  ([^Channel channel ^long prefetch-count]
     (.basicQos channel prefetch-count))
  ([^Channel channel ^long prefetch-size ^long prefetch-count global]
     (.basicQos channel prefetch-size prefetch-count global)))

