;; This source code is dual-licensed under the Apache License, version
;; 2.0, and the Eclipse Public License, version 1.0.
;;
;; The APL v2.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;; ----------------------------------------------------------------------------------
;;
;; The EPL v1.0:
;;
;; ----------------------------------------------------------------------------------
;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;; All rights reserved.
;;
;; This program and the accompanying materials are made available under the terms of
;; the Eclipse Public License Version 1.0,
;; which accompanies this distribution and is available at
;; http://www.eclipse.org/legal/epl-v10.html.
;; ----------------------------------------------------------------------------------

(ns langohr.core
  "Functions that work with connections and shared features between connections
   and channels (e.g. shutdown listeners).

   Relevant guides:

    * http://clojurerabbitmq.info/articles/connecting.html
    * http://clojurerabbitmq.info/articles/tls.html"
  (:import [com.rabbitmq.client Connection Channel Address
            ConnectionFactory ShutdownListener BlockedListener
            Consumer TopologyRecoveryException
            ExceptionHandler Recoverable RecoveryListener DefaultSaslConfig]
           [com.rabbitmq.client.impl ForgivingExceptionHandler AMQConnection]
           [com.rabbitmq.client.impl.recovery AutorecoveringConnection QueueRecoveryListener]
           clojure.lang.IFn
           java.util.concurrent.ThreadFactory)
  (:require langohr.channel
            [clojure.string :as s]
            [clojure.walk   :as walk]))

;;
;; Implementation
;;

(def ^{:dynamic true :doc "Default connection settings."} *default-config*
  {:username "guest"
   :password "guest"
   :vhost     "/"
   :host      "localhost"
   :port      ConnectionFactory/DEFAULT_AMQP_PORT})

;;
;; API
;;

(defprotocol Closeable
  (close [c] "Closes given entity"))

(extend-protocol Closeable
  com.rabbitmq.client.Connection
  (close [this] (.close this))

  com.rabbitmq.client.Channel
  (close [this] (.close this)))


(defprotocol Openable
  (open? [this] "Checks whether given entity is open")
  (closed? [this] "Checks whether given entity is closed"))

(extend-protocol Openable
  com.rabbitmq.client.Connection
  (open? [conn] (.isOpen conn))
  (closed? [conn] (not (.isOpen conn)))

  com.rabbitmq.client.Channel
  (open? [ch] (.isOpen ch))
  (closed? [ch] (not (.isOpen ch))))

(declare create-connection-factory normalize-settings)
(defn- address-array-from
  [addresses port]
  (into-array Address
              (map (fn [arg]
                     (let [[host port] (if (coll? arg)
                                         [(first arg) (second arg)]
                                         [arg port])]
                       (Address. host port)))
                   (remove nil? addresses))))

(defn ^Connection connect
  "Creates and returns a new connection to RabbitMQ."
  ;; defaults
  ([]
     (let [^ConnectionFactory cf (create-connection-factory {})]
       (doto (com.novemberain.langohr.Connection. cf)
         .init)))
  ;; settings
  ([settings]
     (let [settings'             (normalize-settings settings)
           ^ConnectionFactory cf (create-connection-factory settings')
           xs                    (address-array-from (get settings' :hosts #{})
                                                     (get settings' :port ConnectionFactory/DEFAULT_AMQP_PORT))]
       (doto (com.novemberain.langohr.Connection. cf (dissoc settings :password :username))
         (.init xs)))))

(defn ^Channel create-channel
  "Delegates to langohr.channel/open, kept for backwards compatibility"
  [& args]
  (apply langohr.channel/open args))


(defn ^ShutdownListener shutdown-listener
  "Adds new shutdown signal listener that delegates to given function"
  [^clojure.lang.IFn f]
  (reify ShutdownListener
    (shutdownCompleted [this cause]
      (f cause))))

(defn ^ShutdownListener add-shutdown-listener
  "Adds a shutdown listener on connection and returns it"
  [^Connection c ^IFn f]
  (let [lnr (shutdown-listener f)]
    (.addShutdownListener c lnr)
    lnr))

(defn ^BlockedListener blocked-listener
  "Reifies connection.blocked and connection.unblocked listener from Clojure
   functions"
  [^IFn on-blocked ^IFn on-unblocked]
  (reify BlockedListener
    (^void handleBlocked [this ^String reason]
      (on-blocked reason))
    (^void handleUnblocked [this]
      (on-unblocked))))

(defn ^BlockedListener add-blocked-listener
  "Adds a connection.blocked and connection.unblocked listener
   on connection and returns it"
  [^Connection c ^IFn on-blocked ^IFn on-unblocked]
  (let [lnr (blocked-listener on-blocked on-unblocked)]
    (.addBlockedListener c lnr)
    lnr))

(defn settings-from
  "Parses AMQP connection URI and returns a persistent map of settings"
  [^String uri]
  (if uri
    (let [cf (doto (ConnectionFactory.)
               (.setUri uri))]
      {:host     (.getHost cf)
       :port     (.getPort cf)
       :vhost    (.getVirtualHost cf)
       :username (.getUsername cf)
       :password (.getPassword cf)})
    *default-config*))

(defn capabilities-of
  "Returns capabilities of the broker on the other side of the connection"
  [^Connection conn]
  (walk/keywordize-keys (into {} (-> conn .getServerProperties (get "capabilities")))))

;;
;; Recovery
;;

(defn automatic-recovery-enabled?
  "Returns true if provided connection uses automatic connection recovery
   mode, false otherwise"
  [^com.novemberain.langohr.Connection conn]
  (.automaticRecoveryEnabled conn))

(defn ^{:deprecated true} automatically-recover?
  "See automatic-recovery-enabled?"
  [^com.novemberain.langohr.Connection c]
  (automatic-recovery-enabled? c))

(defn automatic-topology-recovery-enabled?
  "Returns true if provided connection uses automatic topology recovery
   mode, false otherwise"
  [^com.novemberain.langohr.Connection conn]
  (.automaticTopologyRecoveryEnabled conn))

(defn on-recovery
  "Registers a network recovery callback on a (Langohr) connection or channel"
  ([^Recoverable target ^IFn recovery-finished-fn]
     (.addRecoveryListener target (reify RecoveryListener
                                    (^void handleRecovery [this ^Recoverable it]
                                      (recovery-finished-fn it))
                                    (^void handleRecoveryStarted [this ^Recoverable it]
                                      ;; intentionally no-op
                                      (fn [this ^Recoverable it] )))))
  ([^Recoverable target ^IFn recovery-started-fn ^IFn recovery-finished-fn]
     (.addRecoveryListener target (reify RecoveryListener
                                    (^void handleRecoveryStarted [this ^Recoverable it]
                                      (recovery-started-fn it))
                                    (^void handleRecovery [this ^Recoverable it]
                                      (recovery-finished-fn it))))))

(defn ^QueueRecoveryListener queue-recovery-listener
  "Reifies a new queue recovery listener that delegates
   to a Clojure function."
  [^IFn f]
  (reify QueueRecoveryListener
    (^void queueRecovered [this ^String old-name ^String new-name]
      (f old-name new-name))))

(defn on-queue-recovery
  "Called when server named queue gets a new name on recovery"
  [^com.novemberain.langohr.Connection conn ^IFn f]
  (.addQueueRecoveryListener (cast AutorecoveringConnection (.getDelegate conn))
                             (queue-recovery-listener f)))


;;
;; Advanced Customization
;;

(defn thread-factory-from
  "Instantiates a java.util.concurrent.ThreadFactory that delegates
   #newThread to provided Clojure function"
  [f]
  (reify java.util.concurrent.ThreadFactory
    (^Thread newThread [this ^Runnable r]
      (f r))))

(defn exception-handler
  [{:keys [handle-connection-exception-fn
           handle-return-listener-exception-fn
           handle-flow-listener-exception-fn
           handle-confirm-listener-exception-fn
           handle-blocked-listener-exception-fn
           handle-consumer-exception-fn
           handle-connection-recovery-exception-fn
           handle-channel-recovery-exception-fn
           handle-topology-recovery-exception-fn]}]
  (proxy [ForgivingExceptionHandler] []
    (handleUnexpectedConnectionDriverException [^Connection conn ^Throwable t]
      (when handle-connection-exception-fn
        (handle-connection-exception-fn conn t)))
    (handleReturnListenerException [^Channel ch ^Throwable t]
      (when handle-return-listener-exception-fn
        (handle-return-listener-exception-fn ch t)))
    (handleFlowListenerException [^Channel ch ^Throwable t]
      (when handle-flow-listener-exception-fn
        (handle-flow-listener-exception-fn ch t)))
    (handleConfirmListenerException [^Channel ch ^Throwable t]
      (when handle-confirm-listener-exception-fn
        (handle-confirm-listener-exception-fn ch t)))
    (handleBlockedListenerException [^Connection conn ^Throwable t]
      (when handle-blocked-listener-exception-fn
        (handle-blocked-listener-exception-fn conn t)))
    (handleConsumerException [^Channel ch ^Throwable t
                              ^Consumer consumer ^String consumer-tag
                              ^String method-name]
      (when handle-consumer-exception-fn
        (handle-consumer-exception-fn ch t consumer consumer-tag method-name)))
    (handleConnectionRecoveryException [^Connection conn ^Throwable t]
      (when handle-connection-recovery-exception-fn
        (handle-connection-recovery-exception-fn conn t)))
    (handleChannelRecoveryException [^Channel ch ^Throwable t]
      (when handle-channel-recovery-exception-fn
        (handle-channel-recovery-exception-fn ch t)))
    (handleTopologyRecoveryException [^Connection conn ^Channel ch
                                      ^TopologyRecoveryException t]
      (when handle-topology-recovery-exception-fn
        (handle-topology-recovery-exception-fn conn ch t)))))

;;
;; Implementation
;;

(defn normalize-settings
  "For setting maps that contain keys such as :host, :username, :vhost, returns the argument"
  [config]
  (let [{:keys [host hosts]} config
        hosts' (into #{} (remove nil? (or hosts #{host})))]
    (merge (settings-from (:uri config (System/getenv "RABBITMQ_URL")))
           {:hosts hosts'}
           config)))

(defn- platform-string
  []
  (let []
    (format "Clojure %s on %s %s"
            (clojure-version)
            (System/getProperty "java.vm.name")
            (System/getProperty "java.version"))))

(def ^{:private true}
  client-properties {"product"      "Langohr"
                     "information"  "See http://clojurerabbitmq.info/"
                     "platform"     (platform-string)
                     "capabilities" (get (AMQConnection/defaultClientProperties) "capabilities")
                     "copyright"    "Copyright (C) 2011-2017 Michael S. Klishin, Alex Petrov"
                     "version"      "4.1.0-SNAPSHOT"})

(defn- auth-mechanism->sasl-config
  [{:keys [authentication-mechanism]}]
  (case authentication-mechanism
    "PLAIN"    DefaultSaslConfig/PLAIN
    "EXTERNAL" DefaultSaslConfig/EXTERNAL
    nil))

(defn- ^ConnectionFactory create-connection-factory
  "Creates connection factory from given attributes"
  [settings]
  (let [{:keys [host port username password vhost
                requested-heartbeat connection-timeout ssl ssl-context socket-factory sasl-config
                requested-channel-max thread-factory exception-handler]
         :or {requested-heartbeat ConnectionFactory/DEFAULT_HEARTBEAT
              connection-timeout  ConnectionFactory/DEFAULT_CONNECTION_TIMEOUT
              requested-channel-max ConnectionFactory/DEFAULT_CHANNEL_MAX
              sasl-config (auth-mechanism->sasl-config settings)}} (normalize-settings settings)
              cf   (ConnectionFactory.)
              final-port (if (and ssl (= port ConnectionFactory/DEFAULT_AMQP_PORT))
                           ConnectionFactory/DEFAULT_AMQP_OVER_SSL_PORT
                           port)]
    (when (or ssl
              (= port ConnectionFactory/DEFAULT_AMQP_OVER_SSL_PORT))
      (.useSslProtocol cf))
    (doto cf
      (.setClientProperties   client-properties)
      (.setUsername           username)
      (.setPassword           password)
      (.setVirtualHost        vhost)
      (.setHost               host)
      (.setPort               final-port)
      (.setRequestedHeartbeat requested-heartbeat)
      (.setConnectionTimeout  connection-timeout)
      (.setRequestedChannelMax requested-channel-max))
    (when sasl-config
      (.setSaslConfig cf sasl-config))
    (when ssl-context
      (.useSslProtocol cf ^javax.net.ssl.SSLContext ssl-context))
    (when thread-factory
      (.setThreadFactory cf ^ThreadFactory thread-factory))
    (if exception-handler
      (.setExceptionHandler cf ^ExceptionHandler exception-handler)
      (.setExceptionHandler cf (ForgivingExceptionHandler.)))
    cf))

