(defproject pseudoace "0.0.3-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
 ;;                [com.datomic/datomic-free "0.9.5186" :exclusions [joda-time]]
                 [com.datomic/datomic-pro "0.9.5344" :exclusions [joda-time]] ;; added
                 [com.amazonaws/aws-java-sdk-dynamodb "1.9.39" :exclusions [joda-time]] ;; added
                 [datomic-schema "1.3.0"]
                 [wormbase/pseudoace "0.3.0"]
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
                 [base64-clj "0.1.1"]
                 [cheshire "5.4.0"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [secretary "1.2.3"]
                 [environ "1.0.0"]]

  :description "REST API for getting data from datomic on a per widget basis"
  :source-paths ["src"]

  :plugins [[lein-environ "1.0.0"]]

  :javac-options ["-target" "1.8" "-source" "1.8"]
  :license "MIT"
  :jvm-opts ["-Xmx6G"
             "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=50"  ;; same GC options as the transactor,
                                                       ;; should minimize long pauses.
             "-Ddatomic.objectCacheMax=2500000000" 
             "-Ddatomic.txTimeoutMsec=1000000"])
