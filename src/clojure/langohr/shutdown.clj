;; Copyright (c) 2011-2013 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.shutdown
  "Convenience functions for dealing with shutdown signals"
  (:import [com.rabbitmq.client ShutdownSignalException Connection Channel]))


;;
;; API
;;

(defn hard-error?
  [^ShutdownSignalException sse]
  (.isHardError sse))

(defn soft-error?
  [^ShutdownSignalException sse]
  (not (.isHardError sse)))

(defn initiated-by-application?
  "Returns true if this if this exception was caused by explicit application
   action; false if it originated with the broker or as a result
   of detectable non-deliberate application failure"
  [^ShutdownSignalException sse]
  (.isInitiatedByApplication sse))

(defn initiated-by-broker?
  "Returns true if this if this exception was initiated by the broker
   because of a channel or connection exception"
  [^ShutdownSignalException sse]
  (not (.isInitiatedByApplication sse)))

(defn reason-of
  [^ShutdownSignalException sse]
  (.getReason sse))

(defn channel-of
  "Returns the channel the signal is associated with. nil if
   shutdown was initiated because of connection closure"
  [^ShutdownSignalException sse]
  (let [r (.getReference sse)]
    (when (instance? Channel r)
      r)))

(defn connection-of
  "Returns connection the signal is associated with"
  [^ShutdownSignalException sse]
  ;; reference is either a connection or channel
  (let [r (.getReference sse)]
    (if (instance? Connection r)
      r
      (.getConnection ^Channel r))))
