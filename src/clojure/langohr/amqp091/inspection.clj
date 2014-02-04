;; Copyright (c) 2011-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.amqp091.inspection
  "Utility functions for protocol method inspection (for development,
   debugging and operations work)"
  (:import [com.rabbitmq.client.impl AMQImpl Method
            ValueWriter MethodArgumentWriter]
           [java.io ByteArrayOutputStream DataOutputStream]))


;;
;; API
;;

(defn ^java.io.ByteArrayOutputStream ->byte-array-output-stream
  "Converts AMQP 0.9.1 method to a byte array output stream"
  [^Method m]
  (let [baos (ByteArrayOutputStream.)
        maw  (MethodArgumentWriter. (ValueWriter. (DataOutputStream. baos)))]
    (.writeArgumentsTo m maw)
    baos))

(defn ->bytes
  "Converts AMQP 0.9.1 method to a byte array"
  [^Method m]
  (let [baos (->byte-array-output-stream m)]
    (.toByteArray baos)))
