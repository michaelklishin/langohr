;; Copyright (c) 2011-2020 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.test.tls-test
  (:require [langohr.core  :as lc]
            [langohr.queue :as lq]
            [langohr.basic :as lb]
            [clojure.test  :refer :all])
  (:import [java.io File FileInputStream]
           java.security.KeyStore
           [javax.net.ssl TrustManagerFactory KeyManagerFactory SSLContext]))

;;
;; Tests
;;

(deftest ^{:tls true} test-connection-with-tls-enabled-and-explicitly-provided-port
  (with-open [conn (lc/connect {:host "127.0.0.1" :port 5671 :ssl true})
              ch   (lc/create-channel conn)]
    (let [q (format "langohr.test.tls-test.%s" (str (java.util.UUID/randomUUID)))]
      (is (lc/open? conn))
      (lq/declare ch q {:exclusive true})
      (lb/publish ch "" q "TLS")
      (let [[_ payload] (lb/get ch q)]
        (is (= (String. ^bytes payload) "TLS"))))))

(deftest ^{:tls true} test-connection-with-tls-enabled-via-uri
  (with-open [conn (lc/connect {:uri "amqps://127.0.0.1:5671/%2F"})
              ch   (lc/create-channel conn)]
    (let [q (format "langohr.test.tls-test.%s" (str (java.util.UUID/randomUUID)))]
      (is (lc/open? conn))
      (lq/declare ch q {:exclusive true})
      (lb/publish ch "" q "TLS")
      (let [[_ payload] (lb/get ch q)]
        (is (= (String. ^bytes payload) "TLS"))))))

(def ^String keystore-path "./tmp/langohr/keystore/keystore")
(def keystore-pwd  (.toCharArray "bunnies"))
(def ^String pkcs12-cert-path "./test/resources/tls/client_key.p12")
(def pkcs12-cert-pwd  (.toCharArray "bunnies"))

(deftest ^{:tls true} test-connection-with-ssl-context
  (let [f (File. keystore-path)]
    (is (.exists f)))
  (let [jks-keystore    (doto (KeyStore/getInstance "JKS")
                          (.load (FileInputStream. keystore-path) keystore-pwd))
        tmf             (doto (TrustManagerFactory/getInstance "SunX509")
                          (.init jks-keystore))
        pkcs12-keystore (doto (KeyStore/getInstance "PKCS12")
                          (.load (FileInputStream. pkcs12-cert-path) pkcs12-cert-pwd))
        kmf             (doto (KeyManagerFactory/getInstance "SunX509")
                          (.init pkcs12-keystore pkcs12-cert-pwd))
        ctx             (doto (SSLContext/getInstance "TLSv1.2")
                          (.init (.getKeyManagers kmf) (.getTrustManagers tmf) nil))
        conn            (lc/connect {:port 5671 :ssl true :ssl-context ctx})
        ch              (lc/create-channel conn)
        q               (format "langohr.test.tls-test.%s" (str (java.util.UUID/randomUUID)))]
    (is (lc/open? ch))
    (lq/declare ch q {:exclusive true})
    (lb/publish ch "" q "connected with TLS")
    (let [[_ payload] (lb/get ch q)]
      (is (= (String. ^bytes payload) "connected with TLS")))
    (lc/close conn)))

(deftest ^{:tls true} test-connection-with-hostname-verification
  (let [f (File. keystore-path)]
    (is (.exists f)))
  (let [jks-keystore    (doto (KeyStore/getInstance "JKS")
                          (.load (FileInputStream. keystore-path) keystore-pwd))
        tmf             (doto (TrustManagerFactory/getInstance "SunX509")
                          (.init jks-keystore))
        pkcs12-keystore (doto (KeyStore/getInstance "PKCS12")
                          (.load (FileInputStream. pkcs12-cert-path) pkcs12-cert-pwd))
        kmf             (doto (KeyManagerFactory/getInstance "SunX509")
                          (.init pkcs12-keystore pkcs12-cert-pwd))
        ctx             (doto (SSLContext/getInstance "TLSv1.2")
                          (.init (.getKeyManagers kmf) (.getTrustManagers tmf) nil))
        conn            (lc/connect {:port 5671 :ssl true :ssl-context ctx :verify-hostname true})
        ch              (lc/create-channel conn)
        q               (format "langohr.test.tls-test.%s" (str (java.util.UUID/randomUUID)))]
    (is (lc/open? ch))
    (lq/declare ch q {:exclusive true})
    (lb/publish ch "" q "connected with TLS")
    (let [[_ payload] (lb/get ch q)]
      (is (= (String. ^bytes payload) "connected with TLS")))
    (lc/close conn)))
