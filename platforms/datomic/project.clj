(defproject smallace-datomic "0.0.1-SNAPSHOT"
  :description "ACeDB/Datomic playground"
  :dependencies [[org.clojure/tools.reader "0.8.3"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/core.match "0.2.1"]
                 [com.datomic/datomic-free "0.9.4899"]
                 [hiccup "1.0.5"]
                 [ring "1.2.1"]
                 [compojure "1.1.6"]
                 [clj-http "0.9.2"]
                 [acetyl "0.0.2-SNAPSHOT"]
                 [datomic-schema "1.1.0"]
                 [com.ninjudd/ring-async "0.3.1"]
                 [com.ninjudd/eventual "0.4.1"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]]
  :repositories [["dasmoth" {:url "http://www.biodalliance.org/people/thomas/repo"}]]
  :jvm-opts ["-Xmx3G" "-Ddatomic.txTimeoutMsec=500000"])
