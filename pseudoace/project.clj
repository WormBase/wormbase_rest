(defproject pseudoace "0.0.3-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [com.datomic/datomic-free "0.9.5153"]
                 [datomic-schema "1.3.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [acetyl "0.0.7-SNAPSHOT"]
                 [com.novemberain/monger "2.0.0"]
                 [org.clojure/tools.reader "0.8.3"]
                 [hiccup "1.0.5"]
                 [ring "1.3.2"]
                 [fogus/ring-edn "0.2.0"]
                 [compojure "1.3.3"]
                 [clj-http "1.1.1"]
                 [com.ninjudd/ring-async "0.3.1"]
                 [com.ninjudd/eventual "0.4.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-2727"]
                 [org.omcljs/om "0.8.7"]
                 [secretary "1.2.1"]
                 [bk/ring-gzip "0.1.1"]
                 [com.cemerick/friend "0.2.1"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [prismatic/om-tools "0.3.10"]
                 [com.andrewmcveigh/cljs-time "0.3.0"]
                 [environ "1.0.0"]]
  :description "ACeDB emulator and migration tools"
  :source-paths ["src/clj" "src/cljs"]
  :java-source-paths ["src/java"]
  :resource-paths ["resources"]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-environ "1.0.0"]]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs"]
              :compiler {
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :source-map true}}]}
  
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :license "MIT"
  :repositories [["dasmoth" {:url "http://www.biodalliance.org/people/thomas/repo"}]]
  :jvm-opts ["-Xmx4G" "-Ddatomic.objectCacheMax=2500000000 -Ddatomic.txTimeoutMsec=1000000"])
