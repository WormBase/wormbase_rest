(defproject datomic-rest-api "0.0.3-SNAPSHOT"
  :description "REST API for retrieving data from datomic on a per widget basis"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-pro "0.9.5359" :exclusions [joda-time]] ;; added
                 [com.amazonaws/aws-java-sdk-dynamodb "1.9.39" :exclusions [joda-time]] ;; added
                 [datomic-schema "1.3.0"]
                 [wormbase/pseudoace "0.4.6"]
                 [hiccup "1.0.5"]
                 [ring "1.4.0"]
                 [fogus/ring-edn "0.2.0"]
                 [compojure "1.4.0"]
                 [clj-http "1.1.1"]
                 [com.ninjudd/ring-async "0.3.1"]
                 [com.ninjudd/eventual "0.4.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [bk/ring-gzip "0.1.1"]
                 [com.cemerick/friend "0.2.1"]
                 [friend-oauth2 "0.1.3"]
                 [org.apache.httpcomponents/httpclient "4.3.6"]
                 [org.clojure/data.json "0.2.0"]
                 [clj-time "0.12.0"]
                 [cheshire "5.6.1"]
                 [secretary "1.2.3"]
                 [environ "1.0.3"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                 :plugins [[lein-midje "3.2"]]}}

  :source-paths ["src"]

  :plugins [[lein-environ "1.0.3"]]

  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "MIT"
  :jvm-opts ["-Xmx6G"
             "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"  ;; same GC options as the transactor,
                                                       ;; should minimize long pauses.
             "-Ddatomic.objectCacheMax=2500000000"
             "-Ddatomic.txTimeoutMsec=1000000"]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}})
