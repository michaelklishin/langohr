(defproject com.novemberain/langohr "1.0.0-SNAPSHOT"
  :description "An experimental Clojure layer on top of the RabbitMQ Java client"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.rabbitmq/amqp-client "2.8.1"]]
  :profiles {:dev { :dependencies [[org.clojure/tools.cli "0.2.1" :exclusions [org.clojure/clojure]]] }
             :1.4 { :dependencies [[org.clojure/clojure "1.4.0"]] }}
  :url "https://github.com/michaelklishin/langohr"
  :repositories {"clojure-releases" "http://build.clojure.org/releases"
                 "sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false,
                             :releases {:checksum :fail :update :always}}}
  :aliases { "all" ["with-profile" "dev:dev,1.4"] }
  :warn-on-reflection true
  :jvm-opts ["-Xmx512m"])
