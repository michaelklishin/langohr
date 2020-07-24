(defproject com.novemberain/langohr "5.2.0-SNAPSHOT"
  :description "An idiomatic Clojure client for RabbitMQ that embraces the AMQP 0.9.1 model. Built on top of the RabbitMQ Java client"
  :min-lein-version "2.5.1"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure      "1.10.1"]
                 [com.rabbitmq/amqp-client "5.8.0"]
                 [clojurewerkz/support     "1.1.0" :exclusions [com.google.guava/guava]]
                 [clj-http                 "3.9.1"]
                 [cheshire                 "5.8.1"]]
  :profiles {:1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :master {:dependencies [[org.clojure/clojure "1.11.0-master-SNAPSHOT"]]}
             :dev {:dependencies [[org.clojure/tools.cli "0.4.1" :exclusions [org.clojure/clojure]]]
                   :resource-paths ["test/resources"]
                   :plugins [[lein-codox "0.10.3"]]
                   :codox {:source-paths ["src/clojure"]
                           :source-uri "https://github.com/michaelklishin/langohr/blob/v{version}/{filepath}#L{line}"}}}
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.8" "-source" "1.8"]
  :url "https://clojurerabbitmq.info"
  :repositories {"sonatype" {:url "https://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :aliases {"all" ["with-profile" "dev:dev,1.9:dev,1.8:dev,master"]}
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
