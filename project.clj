(defproject com.novemberain/langohr "1.0.0-beta10"
  :description "An idiomatic Clojure client for RabbitMQ that embraces AMQP 0.9.1 model. Built on top of the RabbitMQ Java client"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure      "1.4.0"]
                 [com.rabbitmq/amqp-client "2.8.6"]
                 [clojurewerkz/support     "0.7.0"]]
  :profiles {:1.3 { :dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}
             :dev {:dependencies [[org.clojure/tools.cli "0.2.1" :exclusions [org.clojure/clojure]]]
                   :plugins [[codox "0.6.1"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]  
  :url "http://clojurerabbitmq.info"
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :aliases {"all" ["with-profile" "dev:dev,1.3:dev,1.5"]}
  :warn-on-reflection true
  :jvm-opts ["-Xmx512m"])
