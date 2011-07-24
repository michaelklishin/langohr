;; See https://github.com/technomancy/leiningen/blob/stable/sample.project.clj
;; to learn more about available options.
(defproject leporidae "0.1.0-SNAPSHOT"
  :description "An experimental Clojure layer on top of the RabbitMQ Java client"
  :dependencies [[org.clojure/clojure "1.3.0-BETA1"]
                 [com.rabbitmq/amqp-client "2.5.1"]]
  :jvm-opts ["-Xmx512m"])