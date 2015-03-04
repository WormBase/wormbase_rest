(defproject geneace "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [com.datomic/datomic-free "0.9.5130" :exclusions [org.slf4j/slf4j-nop org.slf4j/log4j-over-slf4j]]
                 [datomic-schema "1.1.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.reader "0.8.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-logging-config "1.9.12"]
                 [clj-http "1.0.1"]
                 [org.slf4j/slf4j-log4j12 "1.6.4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.apache.poi/poi-ooxml "3.11"]
                 [instaparse "1.3.5"]
                 [clj-time "0.9.0"]]
  :description "GeneAce-style scripts"
  :source-paths ["src/clj"]
  :resource-paths ["resources"]

  :license "MIT"
  :repositories [["dasmoth" {:url "http://www.biodalliance.org/people/thomas/repo"}]]
  :jvm-opts ["-Xmx6G" "-Ddatomic.txTimeoutMsec=1000000"])
