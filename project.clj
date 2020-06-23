(defproject wormbase/rest-api "1.3.0"
  :description
  "REST API for retrieving data from datomic on a per widget basis"
  :url "https://github.com/WormBase/wormbase-rest"
  :min-lein-version "2.7.0"
  :sign-releases false
  :dependencies
  [[bk/ring-gzip "0.2.1"]
   [cheshire "5.7.0"]
   [com.layerware/hugsql "0.4.7"]
   [environ "1.1.0"]
   [org.clojure/data.xml "0.0.8"]
   [clj-http "3.7.0"]
   [hiccup "1.0.5"]
   [metosin/compojure-api "1.1.10"]
   [mount "0.1.11"]
   [mysql/mysql-connector-java "6.0.5"]
   [org.clojure/math.numeric-tower "0.0.4"]
   [org.biojava/biojava-core "4.2.7"]
   [org.biojava/biojava-aa-prop "4.2.7"]
   [org.clojure/clojure "1.9.0"]
   [org.clojure/data.json "0.2.6"]
   [org.clojure/java.jdbc "0.7.0-alpha1"]
   [org.clojure/tools.reader "1.3.2"]
   [ring "1.7.0"]
   [wormbase/pseudoace "0.6.6"]]
  :source-paths ["src"]
  :plugins [[lein-environ "1.1.0"]
            [lein-pprint "1.1.1"]
            [lein-ring "0.12.5"]]
  :main ^:skip-aot rest-api.main
  :resource-paths ["resources"]
  :uberjar {:aot :all}
  :target-path "target/%s"
  :ring {:handler rest-api.main/app
         :host "0.0.0.0"
         :init rest-api.main/init}
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "GPLv2"
  :jvm-opts ["-Xmx28G"
             ;; same GC options as the transactor,
             ;; should minimize long pauses.
             "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"
             "-Ddatomic.txTimeoutMsec=1000000"]
  :profiles
  {:datomic-free
   {:dependencies [[com.datomic/datomic-free "0.9.5703"
                    :exclusions [joda-time]]]}
   :datomic-pro
   {:dependencies [[com.datomic/datomic-pro "0.9.5703"
                    :exclusions [joda-time]]]}
   :ddb
   {:dependencies
    [[com.amazonaws/aws-java-sdk-dynamodb "1.11.82"]]}
   :dev [:datomic-pro
         :ddb
         {:aliases
          {"code-qa"
           ["do"
            ["eastwood"]
            "test"]}
          :dependencies [[org.clojure/tools.trace "0.7.9"]
                         [ring/ring-devel "1.5.1"]]
          :source-paths ["dev"]
          :jvm-opts ["-Xmx1G"]
          :env
          {:wb-db-uri "datomic:ddb://us-east-1/WS277/wormbase"
           :swagger-validator-url "http://localhost:8002"}
          :plugins
          [[jonase/eastwood "0.2.3"
            :exclusions [org.clojure/clojure]]
           [lein-ancient "0.6.8"]
           [lein-bikeshed "0.3.0"]
           [lein-ns-dep-graph "0.1.0-SNAPSHOT"]
           [venantius/yagni "0.1.4"]
           [com.jakemccrary/lein-test-refresh "0.17.0"]]
          :ring {:nrepl {:start? true}}}]
      :test
      {:resource-paths ["test/resources"]}}
  :repl-options {:init (set! *print-length* 10)})
