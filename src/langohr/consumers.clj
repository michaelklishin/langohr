;; Copyright (c) 2011 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.consumers
  (:refer-clojure :exclude [get])
  (:import (com.rabbitmq.client Channel QueueingConsumer))
  (:use [langohr.basic :as lhb]))


;;
;; API
;;

(defn subscribe
  "Adds new blocking consumer to a queue using basic.consume AMQP method"
  [^Channel channel, ^String queue, ^clojure.lang.IFn message-handler, & { :keys [consumer-tag, auto-ack, exclusive, no-local, arguments]
                                                                         :or { consumer-tag "", auto-ack false, exclusive false, no-local false } }]
  (let [queueing-consumer (QueueingConsumer. channel)]
    (do
      (lhb/consume channel queue queueing-consumer :consumer-tag consumer-tag :auto-ack auto-ack :exclusive exclusive, :arguments arguments, :no-local no-local)
      (while true
        (try
          (let [delivery (.nextDelivery queueing-consumer)]
            (message-handler delivery (.getProperties delivery) (.getBody delivery)))
          (catch InterruptedException e
            nil))))))
