;; See https://github.com/technomancy/leiningen/blob/stable/sample.project.clj
;; to learn more about available options.
(defproject com.novemberain/langohr "0.7.0-SNAPSHOT"
  :description "An experimental Clojure layer on top of the RabbitMQ Java client"
  :url         "https://github.com/michaelklishin/langohr"
  :license     { :name "Eclipse Public License" }
  :repositories { "sonatype"
                 {:url "http://oss.sonatype.org/content/repositories/releases"
                  :snapshots false
                  :releases {:checksum :fail :update :always}
                  }}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.rabbitmq/amqp-client "2.6.1"]]
  :warn-on-reflection true
  :jvm-opts ["-Xmx512m"])