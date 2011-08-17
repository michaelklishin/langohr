(ns langohr.util
  (:import (java.security SecureRandom) (java.math.BigInteger)))


;;
;; API
;;

(defn generate-consumer-tag
  [base]
  (str base "-" (.toString (new BigInteger 130 (SecureRandom.)) 32)))