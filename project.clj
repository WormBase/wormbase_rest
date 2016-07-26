(defproject wormbase/datomic-rest-api "0.0.1"
  :description "REST API for retrieving data from datomic on a per widget basis"
  :url "https://github.com/WormBase/datomic-to-catalyst"
  :min-lein-version "2.0.0"
  :dependencies 
  [[org.clojure/clojure "1.8.0"]
   [com.datomic/datomic-free "0.9.5385"
    :exclusions [joda-time]]
   [datomic-schema "1.3.0"]
   [wormbase/pseudoace "0.4.10"]
   [mount "0.1.10"]
   [hiccup "1.0.5"]
   [ring "1.5.0"]
   [ring/ring-anti-forgery "1.0.1"]
   [ring/ring-jetty-adapter "1.5.0"] 
   [fogus/ring-edn "0.2.0"]
   [compojure "1.4.0"]
   [clj-http "3.1.0"]
   [com.ninjudd/ring-async "0.3.4"]
   [com.ninjudd/eventual "0.4.1"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
   [bk/ring-gzip "0.1.1"]
   [org.apache.httpcomponents/httpclient "4.5.2"]
   [org.clojure/data.json "0.2.0"]
   [clj-time "0.12.0"]
   [cheshire "5.6.1"]
   [secretary "1.2.3"]
   [environ "1.0.3"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-pprint "1.1.1"]
            [lein-ring "0.9.7"]]
  :env {:trace-db "datomic:ddb://us-east-1/wormbase/WS254"
        :trace-port "8120"}
  :main datomic-rest-api.get-handler
  :aot [datomic-rest-api.get-handler]
  :ring {:handler datomic-rest-api.get-handler/app
         :init datomic-rest-api.get-handler/init}
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "GPLv2"
  :jvm-opts ["-Xmx6G"
             "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"  ;; same GC options as the transactor,
                                                       ;; should minimize long pauses.
             "-Ddatomic.objectCacheMax=2500000000"
             "-Ddatomic.txTimeoutMsec=1000000"]
  :profiles {:dev {:dependencies [;;[midje "1.8.3"]
                              ;;    [datomic-schema-grapher "0.0.1"]
                                  [ring/ring-devel "1.5.0"]]
                   :plugins [;;[lein-midje "3.2"]
                             [jonase/eastwood "0.2.3"]
                             [lein-ancient "0.6.8"]
                             [lein-bikeshed "0.3.0"]
                             [lein-kibit "0.1.2"]
                             [lein-ns-dep-graph "0.1.0-SNAPSHOT"]]}
              :datomic-pro {:dependencies [[com.datomic/datomic-pro "0.9.5385"
                                            :exclusions [joda-time]]]
                           :exclusions [com.datomic/datomic-free]}
              :ddb {:dependencies [[com.amazonaws/aws-java-sdk-dynamodb "1.11.6"
                                    :exclusions [joda-time]]]}})
