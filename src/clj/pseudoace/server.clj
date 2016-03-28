(ns pseudoace.server
  (:use pseudoace.pace
        pseudoace.model2schema)
  (:import [pseudoace AceSocketServer AceSessionHandlerFactory AceSessionHandler]
           [pseudoace.pace AceNode]
           [java.io StringWriter])
  (:require [clojure.string :as str]
            [datomic.api :as d :refer (q db entity touch)]
            [clojure.tools.cli :refer (parse-opts)]))

(def ^:dynamic *con* (d/connect "datomic:free://localhost:4334/pitest2"))

(def ace-show-options
  [["-h" "--human-readable-format"]
   ["-a" "--ace-format"]
   ["-p" "--perl-format"]
   ["-j" "--java-format"]
   ["-T" "--include-timestamps"]
   ["-b" "--start begin" "First entry in keyset to show"
    :default 0
    :parse-fn #(Integer/parseInt %)]
   ["-c" "--count max" "Maximum number of entries to show"
    :default 320000000
    :parse-fn #(Integer/parseInt %)]
   ["-f" "--outfile filename"]
   ["-C" "--ignore-comments"]
   ["-t" "--tag tag"]])

(def ace-list-options
  [["-j" "--java-format"]
   ["-b" "--start begin" "First entry in keyset to show"
    :default 0
    :parse-fn #(Integer/parseInt %)]
   ["-c" "--count max" "Maximum number of entries to show"
    :default 320000000
    :parse-fn #(Integer/parseInt %)]])

(defn- third [coll]
  (first (drop 2 coll)))

(deftype PAceSession
    [keyset clazz]
  AceSessionHandler
  (transact [this session cmd]
    (println cmd)
    (let [ctoks (str/split (.trim cmd) #"\s+")
          ctoks (if (= (first ctoks) "query")
                  (rest ctoks)
                  ctoks)
          ddb   (db *con*)
          w     (StringWriter.)]
      (case (.toLowerCase (first ctoks))
        "find"
        (do
          (let [fc    (second ctoks)
                name  (str/replace (or (third ctoks) "*") #"\"" "")]
            (if-let [class-ident (ffirst
                                    (q '[:find ?i
                                          :in $ ?clazz
                                          :where [?a :pace/identifies-class ?ic]
                                                 [(.equalsIgnoreCase ^String ?clazz ^String ?ic)]
                                                 [?a :db/ident ?i]]
                                       ddb fc))]
              (do
                (reset! clazz class-ident)
                (reset! keyset
                  (mapv #(entity ddb (first %))
                    (if (not= name "*")
                      (q '[:find ?e
                           :in $ ?key ?name
                           :where [?e ?key ?name]]
                         ddb class-ident name)
                      (q '[:find ?e
                           :in $ ?key
                           :where [?e ?key _]]
                         ddb class-ident))))
                (binding [*out* w]
                  (println)
                  (print "// Found" (count @keyset) "objects in this class"))))))

        

        "list"
        (let [{:keys [options arguments summary errors]}
              (parse-opts (rest ctoks) ace-show-options)]
          (when-let [class-ident @clazz]
            (binding [*out* w]
              (if (:java-format options)
                (let [objs (->> @keyset
                                 (drop (dec (:start options)))
                                 (take (:count options)))
                      classname (->> (entity ddb @clazz)
                                 (:pace/identifies-class))]
                  (println)
                  (println "KeySet : Answer_1")
                  (doseq [obj objs]
                    (println (str "?" classname "?" (class-ident obj) "?")))
                  (println)
                  (println)
                  (print "//" (count objs) "object listed"))
                (doseq [obj (->> @keyset
                                 (drop (dec (:start options)))
                                 (take (:count options)))]
                  (println " " (class-ident obj)))))))
   

        "show"
        (let [{:keys [options arguments summary errors]}
                (parse-opts (rest ctoks) ace-show-options)
              format (or (some #{:human-readable-format
                                 :ace-format
                                 :perl-format
                                 :java-format}
                               (keys options))
                         :human-readable-format)
              tag (or (:tag options) (first arguments))]
          (binding [*out* w]
            (let [objs (->> @keyset
                            (drop (dec (:start options)))
                            (take (:count options)))]
              (doseq [obj objs]
                (print-ace format (if tag
                                    (objectify @clazz obj tag)
                                    (objectify @clazz obj))))
              (print "//" (count objs) "object dumped"))))

        "model"
        (when-let [name (second ctoks)]
          (if-let [class-ident (ffirst
                                (q '[:find ?i
                                     :in $ ?clazz
                                     :where [?a :pace/identifies-class ?ic]
                                     [(.equalsIgnoreCase ^String ?clazz ^String ?ic)]
                                     [?a :db/ident ?i]]
                                   ddb name))]
            (binding [*out* w]
              (print-model (schema->model ddb class-ident)))))

        "follow"
        (when-let [tag (second ctoks)]
          (let [follows (potential-follows ddb tag)
                newkeyset (transient #{})]
            (println follows)
            (doseq [k @keyset]
              (doseq [f follows
                      :let [[n t] (f k)]
                      :when (seq n)]
                (reset! clazz t)
                (doseq [nn n]
                  (conj! newkeyset nn))))
            (reset! keyset (vec (persistent! newkeyset)))))
        
        "quit"
        (.close session)
        
        ;; default
        (binding [*out* w]
          (println "Don't understand " cmd)))

     (println (str "// " (count @keyset) " active objects"))
     (str/join "\n"
               [(.toString w)
                (str "// " (count @keyset) " Active Objects")]))))
              
(defn make-pace-session []
  (PAceSession. (atom []) (atom nil)))

(defn run-server []
  (AceSocketServer.
   23001
   (reify
    AceSessionHandlerFactory
    (createSession [_]
      (println "Making session")
      (make-pace-session)))))

(defn -main [& args]
  (println "Hello")
  )
