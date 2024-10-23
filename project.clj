(defproject com.novemberain/langohr "6.0.0-SNAPSHOT"
  :description "A Clojure client for RabbitMQ that embraces the underlying protocol. Built on top of the RabbitMQ Java client"
  :min-lein-version "2.5.1"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure      "1.12.0"]
                 [com.rabbitmq/amqp-client "5.22.0"]
                 [clojurewerkz/support     "1.5.0" :exclusions [com.google.guava/guava]]
                 [clj-http                 "3.13.0"]
                 [hato                     "1.0.0"]
                 [cheshire                 "5.13.0"]]
  :profiles {:1.10 {:dependencies [[org.clojure/clojure "1.10.2"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :master {:dependencies [[org.clojure/clojure "1.12.0-master-SNAPSHOT"]]}
             :dev {:dependencies [[org.clojure/tools.cli "1.1.230" :exclusions [org.clojure/clojure]]]
                   :resource-paths ["test/resources"]
                   :plugins [[lein-codox "0.10.8"]]
                   :codox {:source-paths ["src/clojure"]
                           :output-path "./docs"
                           :source-uri "https://github.com/michaelklishin/langohr/blob/v{version}/{filepath}#L{line}"}}}
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "11" "-source" "11"]
  :url "https://clojurerabbitmq.info"
  :repositories {"sonatype" {:url "https://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :deploy-repositories {"releases" {:url "https://repo.clojars.org" :creds :gpg}}
  :aliases {"all" ["with-profile" "dev:dev,1.9:dev,1.10:dev,master"]}
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
                   :ci             (complement :tls)}
  :mailing-list {:name "clojure-rabbitmq"
                 :archive "https://groups.google.com/group/clojure-rabbitmq"
                 :post "clojure-rabbitmq@googlegroups.com"})
