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

(ns langohr.shutdown
  "Convenience functions for dealing with shutdown signals"
  (:import [com.rabbitmq.client ShutdownSignalException Connection Channel]))


;;
;; API
;;

(defn hard-error?
  "Returns true if the ShutDownSignalException signals a connection
   error"
  [^ShutdownSignalException sse]
  (.isHardError sse))

(defn soft-error?
  "Returns true if the ShutdownSignalException does not signal a
  connection error"
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
