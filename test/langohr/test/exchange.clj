(set! *warn-on-reflection* true)

(ns langohr.test.exchange
  (:import (com.rabbitmq.client Connection Channel AMQP  AMQP$Exchange$DeclareOk))
  (:use [clojure.test] [langohr.core :as lhc] [langohr.exchange :as lhe]))


;;
;; exchange.declare
;;

