(defproject wormbase/rest-api "0.1.0"
  :description
  "REST API for retrieving data from datomic on a per widget basis"
  :url "https://github.com/WormBase/datomic-to-catalyst"
  :min-lein-version "2.7.0"
  :sign-releases false
  :dependencies
  [[bk/ring-gzip "0.2.1"]
   [cheshire "5.7.0"]
   [clj-http "3.4.1"]
   [clj-time "0.13.0"]
   [com.layerware/hugsql "0.4.7"]
   [com.ninjudd/eventual "0.5.5"]
   [com.ninjudd/ring-async "0.3.4"]
   [compojure "1.5.2"]
   [datomic-schema "1.3.0"]
   [environ "1.1.0"]
   [fogus/ring-edn "0.3.0"]
   [hiccup "1.0.5"]
   [metosin/compojure-api "1.1.10"]
   [mount "0.1.11"]
   [mysql/mysql-connector-java "6.0.5"]
   [org.apache.httpcomponents/httpclient "4.5.3"]
   [org.clojure/clojure "1.8.0"]
   [org.clojure/core.async "0.2.395"]
   [org.clojure/data.json "0.2.6"]
   [org.clojure/java.jdbc "0.7.0-alpha1"]
   [ring "1.5.1"]
   [ring/ring-anti-forgery "1.0.1"]
   [ring/ring-jetty-adapter "1.5.1"]
   [secretary "1.2.3"]
   [wormbase/pseudoace "0.4.14"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-pprint "1.1.1"]
            [lein-ring "0.9.7"]]
  :main ^:skip-aot rest-api.main
  :resource-paths ["resources"]
  :uberjar {:aot :all}
  :ring {:handler rest-api.main/app
         :host "0.0.0.0"
         :init rest-api.main/init}
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "GPLv2"
  :jvm-opts ["-Xmx6G"
             ;; same GC options as the transactor,
             ;; should minimize long pauses.
             "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"
             "-Ddatomic.objectCacheMax=2500000000"
             "-Ddatomic.txTimeoutMsec=1000000"]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.5.1"]]
                   :source-paths ["dev"]
                   :env
                   {:trace-db "datomic:ddb://us-east-1/WS257/wormbase"}
                   :plugins [[jonase/eastwood "0.2.3"]
                             [lein-ancient "0.6.8"]
                             [lein-bikeshed "0.3.0"]
                             [lein-ns-dep-graph "0.1.0-SNAPSHOT"]
                             [venantius/yagni "0.1.4"]
                             [com.jakemccrary/lein-test-refresh "0.17.0"]]
                   :eastwood {:add-linters [:unused-namespaces]
                              :exclude-namespaces [user]}
		   :ring {:nrepl {:start? true}}}
             :datomic-free
             {:dependencies [[com.datomic/datomic-free "0.9.5554"
                              :exclusions [joda-time]]]}
             :datomic-pro
             {:dependencies [[com.datomic/datomic-pro "0.9.5554"
                              :exclusions [joda-time]]]}
             :ddb
             {:dependencies
              [[com.amazonaws/aws-java-sdk-dynamodb "1.11.6"
                :exclusions [joda-time]]]}}
  :repl-options {:init (set! *print-length* 10)})
