(defproject pseudoace "0.0.2-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [com.datomic/datomic-free "0.9.5067"]
                 [datomic-schema "1.1.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [acetyl "0.0.4-SNAPSHOT"]
                 [com.novemberain/monger "2.0.0"]]
  :description "ACeDB emulator and migration tools"
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :license "MIT"
  :url "http://www.biodalliance.org/people/thomas"
  :repositories [["dasmoth" {:url "http://www.biodalliance.org/people/thomas/repo"}]]
  :jvm-opts ["-Xmx3G" "-Ddatomic.txTimeoutMsec=1000000"])
