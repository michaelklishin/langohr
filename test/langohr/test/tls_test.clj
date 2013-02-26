(ns langohr.test.tls-test
  (:require [langohr.core  :as lc]
            [langohr.queue :as lq]
            [langohr.basic :as lb])
  (:use clojure.test)
  (:import [java.io File FileInputStream]
           java.security.KeyStore
           [javax.net.ssl TrustManagerFactory KeyManagerFactory SSLContext]))

;;
;; Unverified
;;

(deftest ^{:tls true} test-connection-without-peer-verification
  (let [conn (lc/connect {:host "127.0.0.1" :ssl true})
        ch   (lc/create-channel conn)
        q    (format "langohr.test.tls-test.%s" (str (java.util.UUID/randomUUID)))]
    (is (lc/open? conn))
    (lq/declare ch q :exclusive true)
    (lb/publish ch "" q "TLS")
    (let [[_ payload] (lb/get ch q)]
      (is (= (String. ^bytes payload) "TLS")))
    (lc/close conn)))

;;
;; Verified
;;

(def ^String keystore-path "./tmp/langohr/keystore/keystore")
(def keystore-pwd  (.toCharArray "bunnies"))
(def ^String pkcs12-cert-path "./test/resources/tls/client/keycert.p12")
(def pkcs12-cert-pwd  (.toCharArray "bunnies"))

(deftest test-connection-with-peer-verification
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
        ctx             (doto (SSLContext/getInstance "SSLv3")
                          (.init (.getKeyManagers kmf) (.getTrustManagers tmf) nil))
        conn            (lc/connect {:port 5671 :ssl true :ssl-context ctx})
        ch              (lc/create-channel conn)
        q               (format "langohr.test.tls-test.%s" (str (java.util.UUID/randomUUID)))]
    (is (lc/open? ch))
    (lq/declare ch q :exclusive true)
    (lb/publish ch "" q "verified TLS")
    (let [[_ payload] (lb/get ch q)]
      (is (= (String. ^bytes payload) "verified TLS")))
    (lc/close conn)))
