(defproject com.novemberain/langohr "2.12.0-SNAPSHOT"
  :description "An idiomatic Clojure client for RabbitMQ that embraces AMQP 0.9.1 model. Built on top of the RabbitMQ Java client"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure      "1.6.0"]
                 [com.rabbitmq/amqp-client "3.3.1"]
                 [clojurewerkz/support     "0.20.0"]
                 [clj-http                 "0.9.1"]
                 [cheshire                 "5.3.1"]]
  :profiles {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :master {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}
             ;; this version of clj-http depends on HTTPCore 4.2.x which
             ;; some projects (e.g. using Spring's RestTemplate) can rely on,
             ;; so we test for compatibility with it. MK.
             :cljhttp076 {:dependencies [[clj-http "0.7.6"]]}
             :dev {:dependencies [[org.clojure/tools.cli "0.3.1" :exclusions [org.clojure/clojure]]]
                   :resource-paths ["test/resources"]
                   :plugins [[codox "0.8.9"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"]
  :url "http://clojurerabbitmq.info"
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :aliases {"all" ["with-profile" "dev:dev,1.4:dev,1.5:dev,master:dev,cljhttp076:dev,1.5,cljhttp076"]}
  :global-vars {*warn-on-reflection* true}
  :jvm-opts ["-Xmx512m"]
  :test-selectors {:default        (fn [m]
                                     (and (not (:performance m))
                                          (not (:edge-features m))
                                          (not (:time-consuming m))
                                          (not (:tls m))))
                   :http           :http
                   :focus          :focus
                   ;; as in, edge rabbitmq server
                   :edge-features  :edge-features
                   :time-consuming :time-consuming
                   :performance    :performance
                   :tls            :tls
                   :ci             (fn [m] (not (:tls m)))}
  :mailing-list {:name "clojure-rabbitmq"
                 :archive "https://groups.google.com/group/clojure-rabbitmq"
                 :post "clojure-rabbitmq@googlegroups.com"})
