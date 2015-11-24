(ns ace-to-datomic
  (:require [pseudoace.model :as model]
            [pseudoace.model2schema :as model2schema] 

            [datomic.api :as datomic]
            [pseudoace.metadata-schema :as metadata-schema]
            [pseudoace.locatable-schema :as locatable-schema]
            [pseudoace.wormbase-schema-fixups :as wormbase-schema-fixups]

            [pseudoace.schema-datomic :as schema-datomic]
            [pseudoace.utils :as utils]
            [clojure.pprint :as pp]

            [pseudoace.import :as old-import]
            [pseudoace.ts-import :as ts-import]
            [pseudoace.locatable-import :as loc-import]
            [acetyl.parser :as ace]

            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]

            [clojure.test :as t]
            [clojure.java.shell :as shell])
  (:import (java.lang.Runtime)
           (java.net InetAddress) 
           (java.io.FileInputStream) 
           (java.io.File)
           (java.util.zip.GZIPInputStream)
           (java.lang.Runtimea))
  (:gen-class))

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   [nil "--model PATH" "Specify the model file that you would like to use that is found in the models folder e.g. models.wrm.WS250.annot"]
   [nil "--url URL" "Specify the url of the Dataomic transactor you would like to connect. Example: datomic:free://localhost:4334/WS250"]
   [nil "--schema-filename PATH" "Specify the name of the file for the schema view to be written to when selecting Action: generate-schema-view exampls schema250.edn"]
   [nil "--log-dir PATH" "Specifies the path to and empty directory to store the Datomic logs in. Example: /datastore/datomic/tmp/datomic/import-logs-WS250/"]
   [nil "--acedump-dir PATH" "Specifies the path to the directory of the desired acedump. Example /datastore/datomic/tmp/acedata/WS250/"]
   [nil "--backup-file PATH" "Secify the path to the file in which you would like to have the database dumped into"]
   [nil "--datomic-database-report-filename PATH" "Specify the relative or full path to the file that you would like the report to be written to"]
   ["-v" "--verbose"]
   ["-f" "--force"]
   ["-h" "--help"]])

(defn usage [options-summary]
 (->> ["Ace to dataomic is tool for importing data from ACeDB into to Datomic database"
       ""
       "Usage: ace-to-datomic [options] action"
       ""
       "Options:"
       options-summary
       ""
       "Actions: (required options for each action are provided in square brackets)"
       "  create-database                      Select this option if you would like to create a Datomic database from a schema. Required options [model, url]"
       "  create--helper-database              Select this option if you would like to create a helper Datomic database from a schema. Required options [model, url]"
       "  generate-datomic-schema-view         Select if you would like the schema to the database to be exported to a file. Required options [schema-filename, url]" 
       "  acedump-to-datomic-log               Select if you are importing data from ACeDB to Datomic and would like to create the Datomic log files [url, log-dir, acedump-dir]"
       "  sort-datomic-log                     Select if you would like to sort the log files generated from your ACeDB dump [log-dir]"
       "  import-logs-into-datomic             Select if you would like to import the sorted logs into datomic [log-dir, url]"
       "  import-helper-log-into-datomic       Select if you would like to import the helper log into datomic [log-dir, url]"
       "  excise-tmp-data                      Select in order to remove all the tmp data that was created in the database to import the data [url]"
       "  test-datomic-data                    Select if you would like to perform tests on the generated database [url acedump-dir]"
       "  all-import-actions                   Select if you would like to perform all actions from acedb to datomic [model url schema-filename log-dir acedump-dir]"
       "  generate-datomic-database-report     Select if you want to generate a summary report of the contents of a particular Datomic database [url datomic-database-report-filename]"
       "  list-databases                       Select if you would like to get a list of the database names [url]"
       "  delete-database                      Select this option if you would like to delete a database in datomic [url]. If the force option you will not be asked if you are certain about your decision"
       "  backup-database                      Select if you would like to backup a datomic database into a file"]
       (string/join \newline)))

(defn error-msg [errors]
   (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
   (println msg)
   (System/exit status))

(defn run-delete-database [options]
    (datomic/delete-database (:url options))
    (if (:verbose options) (println "deleting database: " (:url options))))

(defn check-if-delete [url]
   (println "Are you sure you would like to delete the database: ", url, " [y/n]")
   (read-line))

(defn delete-database [options]
    (if (or (:force options) (= (check-if-delete (:url options)) (str "y")))
         (run-delete-database options)
         (println "not deleting database")))

(defn generate-datomic-schema-view [options]
    (if (:verbose options) (println "Generating Datomic schema view"))
    (if (:verbose options) (println "\tCreating database connection"))
    (def uri (:url options))
    (def con (datomic/connect uri))
    (utils/with-outfile (:schema-filename options)
    (doseq [s (schema-datomic/schema-from-db (datomic/db con))]
        (pp/pprint s)
        (println))
    (if (:verbose options) (println "\tReleasing database connection"))
    (datomic/release con)))

(defn generate-schema [options]
    (if (:verbose options) (println "\tGenerating Schema"))
    (if (:verbose options) (println "\tRead in annotated ACeDB models file generated by hand - PAD to create this"))
    (def models (model/parse-models (io/reader (string/join "" ["models/" (:model options)]))))
    (if (:verbose options) (println "\tMaking the datomic schema from the acedb annotated models"))
    (def schema (model2schema/models->schema models)))

(defn add-schema-attribute-to-datomic-schema [options con schema tx-quiet]
   (if (:verbose options) (println "\tAdding extra attribute 'schema' to list of attributes and add timestamp to preserve ACeDB timeseamps with auto-generated schema"))
   (try
       (tx-quiet con (conj schema
                    {:db/id          #db/id[:db.part/tx]
                     :db/txInstant   #inst "1970-01-01T00:00:01"}))
       (catch Exception e(str "Caught Exception: " (.getMessage e))))) 

(defn load-schema [uri options]
    (if (:verbose options) (println (string/join " " ["Loading Schema into:" uri])))
    (if (:verbose options) (println "\tCreating database connection"))
    (def con (datomic/connect uri))
    ;; define function tx-quiet that runs a transaction, 
    ;; ensures it completes and throws away all of the output so it runs quietly
    (defn tx-quiet
       "Run a transaction but suppress the (potentially-large) report if it succeeds." 
       [con tx]
       @(datomic/transact con tx)
       nil)
    ;; Built-in schemas include explicit 1970-01-01 timestamps.
    ;; the 'metaschema' and 'locatable-schema' lines simply execute
    ;; what was read in on the previous two lines for metadata and locatables
    (tx-quiet con metadata-schema/metaschema)      ; pace namespace, used by importer
    ;;  this is also from the metadata package
    (tx-quiet con metadata-schema/basetypes)       ; Datomic equivalents for some ACeDB builtin types
    (tx-quiet con locatable-schema/locatable-schema)
    ;; add an extra attribute to the 'schema' list of schema attributes, 
    ;; saying this transaction occurs on 1st Jan 1970 to fake a first 
    ;; transaction to preserve the acedb timestamps
    (add-schema-attribute-to-datomic-schema options con schema tx-quiet)
    (if (:verbose options) (println "\tAdding locatables-extras"))
    (tx-quiet con locatable-schema/locatable-extras) ; pace metadata for locatable schema,
                                    ; needs to be transacted after the main
                                    ; schema.
    (if (:verbose options) (println "\tAdding wormbase-schema-fixups"))
    (tx-quiet con wormbase-schema-fixups/schema-fixups)
    (if (:verbose options) (println "\tReleasing database connection"))
    (datomic/release con))
 
(defn create-database [options]
    (if (:verbose options) (println "Creating Database"))
    (generate-schema options)
    (def uri (:url options))
    (datomic/create-database uri)
    (load-schema uri options))

(defn uri-to-helper-uri [uri]
     (string/join "-" [uri "helper"]))

(defn create-helper-database [options]
    (if (:verbose options) (println "Creating Helper Database"))
    (def helper_uri (uri-to-helper-uri (:url options)))
    (datomic/create-database helper_uri))

(defn directory-walk [directory pattern]
  (doall (filter #(re-matches pattern (.getName %))
                 (file-seq (io/file directory)))))

(defn get-ace-files [directory]
    (map #(.getPath %) (directory-walk directory #".*\.ace.gz")))

(defn get-datomic-log-files [directory]
    (map #(.getName %) (directory-walk directory #".*\.edn.gz")))

(defn get-datomic-sorted-log-files [log-dir]
    (->> (.listFiles (io/file log-dir))
        (filter #(.endsWith (.getName %) ".edn.sort.gz"))
        (sort-by #(.getName %))))

(def not-nil? (complement nil?))

(defn acedump-file-to-datalog [imp file log-dir verbose]
    (if (not-nil? verbose)  (println "\tConverting " file))
    ;; then pull out objects from the pipeline in chunks of 20 objects. 
    ;; Larger block size may be faster if you have plenty of memory
    (doseq [blk (->> (java.io.FileInputStream. file)
                     (java.util.zip.GZIPInputStream.)
                     (ace/ace-reader)
                     (ace/ace-seq)
                     (partition-all 20))] 
           (ts-import/split-logs-to-dir imp blk log-dir)))

(defn helper-file [] "helper.edn.gz")

(defn helper-folder [log-dir]
    (string/join "" [log-dir "helper/"]))

(defn helper-dest [log-dir]
    (def helper_folder (helper-folder log-dir ))
    (string/join "" [(helper-folder log-dir) (helper-file)]))

(defn move-helper-log-file [options]
    (if (:verbose options) (println "\tMoving helper log file"))
    (.mkdir (java.io.File.  (helper-folder (:log-dir options))))
    (def source       (string/join "" [(:log-dir options) "/" (helper-file)]))
    (io/copy (io/file source) (io/file (helper-dest (:log-dir options))))
    (io/delete-file (io/file source)))

(defn acedump-to-datomic-log [options]
    (if (:verbose options) (println "Converting ACeDump to Datomic Log"))
    (if (:verbose options) (println "\tCreating database connection"))
    (def uri (:url options))
    (def con (datomic/connect uri))
    (def imp (old-import/importer con)) ;; Helper object, holds a cache of schema data.
    (def log-dir (io/file (:log-dir options)))   ;; Must be an empty directory
    (def files (get-ace-files (:acedump-dir options)))
    (doseq [file files] (acedump-file-to-datalog imp file log-dir (:verbose options)))

    (move-helper-log-file options)
    (if (:verbose options) (println "\tReleasing database connection"))
    (datomic/release con))

(defn remove-from-end [s end]
  (if (.endsWith s end)
      (.substring s 0 (- (count s)
                         (count end)))
    s))

(defn check-sh-result [result options]
    (if-not (zero? (:exit result)) 
        (println "ERROR: Sort command had exit value: " (:exit result) " and err: " (:err result) ) 
        (if (:verbose options) (println "Sort completed Successfully"))))

(defn get-current-directory []
  (. (java.io.File. ".") getCanonicalPath))

(defn sort-datomic-log-command [file]
    (def script (string/join "" [(get-current-directory) "/src/perl/sort-edn-log.pl"]))
    (shell/sh "./src/perl/sort-edn-log.pl" file :dir (get-current-directory)))

(defn sort-datomic-log [options]
    (if (:verbose options) (println "sorting datomic log"))
    (def files (get-datomic-log-files (:log-dir options)))
    (doseq [file files] 
        (if (:verbose options) (println "sorting file " file))
        (def filepath (string/join "" [(:log-dir options) file]))
        (def result (sort-datomic-log-command filepath))
        (check-sh-result result options)))

(defn import-logs-into-datomic [options]
    (if (:verbose options) (println "importing logs into datomic"))
    (def uri (:url options))
    (def con (datomic/connect uri))
    (def log-files (get-datomic-sorted-log-files (:log-dir options)))
    (doseq [file log-files]
       (if (:verbose options) (println "\timporting: " (.getName file)))
       (ts-import/play-logfile con (java.util.zip.GZIPInputStream. (io/input-stream file))))
    (if (:verbose options) (println "\treleasing database connection"))
    (datomic/release con))

(defn excise-tmp-data [options]
    (def uri (:url options))
    (def con (datomic/connect uri))
    (datomic/transact con [{:db/id #db/id[:db.part/user] :db/excise :importer/temp}]))

(defn test-datomic-data [options]
    (if (:verbose options) (println "testing datomic data"))
    (def uri (:url options))
    (def con (datomic/connect uri))
    (def result_one  (datomic/q '[:find ?c :in $ :where [?c :gene/id "WBGene00018635"]] (datomic/db con)) )
    (t/is (= #{[923589767780191]} result_one)) ;; actually got 936783933079204
    (if (:verbose options) (println "\treleasing database connection"))
    (datomic/release con))


(defn import-helper-log-into-datomic [options]
    (if (:verbose options) (println "importing helper log into helper database"))
    (def helper-uri (uri-to-helper-uri (:url options)))
    (def helper-connection (datomic/connect helper-uri))
    (ts-import/binding [*suppress-timestamps* true]
        (ts-import/play-logfile helper-connection (helper-dest (:log-dir options)))
    (if (:verbose options) (println "\treleasing helper database connection"))
    (datomic/release helper-connection))

(defn helper-file-to-datalog [helper-db imp file log-dir verbose]
    (if (not-nil? verbose)  (println "\tAdding extra data from: " file))
    ;; then pull out objects from the pipeline in chunks of 20 objects. 
    (doseq [blk (->> (java.io.FileInputStream. file)
                     (java.util.zip.GZIPInputStream.)
                     (ace/ace-reader)
                     (ace/ace-seq)
                     (partition-all 20))] ;; Larger block size may be faster if
                                          ;; you have plenty of memory.
          (loc-import/split-locatables-to-dir helper-db imp blk log-dir)))

(defn run-locatables-importer-for-helper [options]
    (if (:verbose options) (println "importing logs with loactables importer into helper database"))
    (def helper-uri (uri-to-helper-uri (:url options)))
    (def helper-connection (datomic/connect helper-uri))
    (def helper-db (datomic/db helper-connection))
    (def imp (old-import/importer helper-connection)) ;; Helper object, holds a cache of schema data.
    (def log-dir (:log-dir options))
    (def files (get-ace-files (:acedump-dir options)))
    (doseq [file files] (helper-file-to-datalog helper-db imp file log-dir (:verbose options)))
    (if (:verbose options) (println "\treleasing helper database connection"))
    (datomic/release helper-connection))

(defn delete-helper-database [options]
    (if (:verbose options) (println "deleting helper database"))
    (def helper_uri (uri-to-helper-uri (:url options)))
    (datomic/delete-database helper_uri))

(defn all-import-actions [options]
;;    (acedump-to-datomic-log options)
;;    (create-helper-database options)
;;      (create-database options)
      (generate-datomic-schema-view options)
      (import-helper-log-into-datomic options)
;;    (run-locatables-importer-for-helper options)
;;    (delete-helper-database options)
;;    (sort-datomic-log options)
;;    (import-logs-into-datomic options)
;;    (excise-tmp-data options)
;;    (test-datomic-data options))
)
(defn list-databases [options]
    (doseq [database-name (datomic/get-database-names (:url options))]
          (println database-name)))    

(defn generate-datomic-database-report [options]
    (if (:verbose options) (println "Generateing Datomic database report"))
    (def uri (:url options))
    (def con (datomic/connect uri))
    (def db (datomic/db con))

    (def filename (:datomic-database-report-filename options))
    (println filename)

    (def elements-attributes (sort (datomic/q '[:find [?ident ...]
                                               :where [_ :db/ident ?ident]]
                                             db)))

    (with-open [wrtr (io/writer (:datomic-database-report-filename options))]
        (binding [*out* wrtr]
            (print "element" "\t" "attribute" "\t" "count")
            (doseq [element-attribute elements-attributes]
                (def element (namespace element-attribute))
                (def attribute (name element-attribute))
                (def expression [:find '(count ?eid) '.
                                 :where ['?eid element-attribute]])
                (def name-of-entity (datomic/q expression db ))
                (def line (str element "\t" attribute "\t" name-of-entity))
                (println line))))

    (if (:verbose options) (println "\treleasing database connection"))
    (datomic/release con))

(defn backup-database [options]
    (if (:verbose options) (println "backing up database"))
    (def uri (:url options))
    (def con (datomic/connect uri))
    (if (:verbose options) (println "\treleasing database connection"))
    (datomic/release con))

(defn -main [& args]
    (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
        (:help options) (exit 0 (usage summary))
        (not= (count arguments) 2) (exit 1 (usage summary))
        errors (exit 1 (error-msg errors)))
    (case (last arguments)
        "acedump-to-datomic-log"            (if (or (string/blank? (:url options)) 
                                                    (string/blank? (:log-dir options)) 
                                                    (string/blank? (:acedump-dir options)))
                                                (println "Options url and log-dir and ace-dump-dir are required for converting the acedump into datomic-log format")
                                                (acedump-to-datomic-log options))
        "create-helper-database"            (if (or (string/blank? (:model options)) 
                                                    (string/blank? (:url options)))  
                                                (println "Options url and model are required when creating helper database")  
                                                (create-helper-database options))
        "create-database"                   (if (or (string/blank? (:model options)) 
                                                    (string/blank? (:url options)))  
                                                (println "Options url and model are required when creating database")  
                                                (create-database options))
        "generate-datomic-schema-view"      (if (or (string/blank? (:schema-filename options)) 
                                                    (string/blank? (:url options)))
                                                (println "Options --url and --schema-filename are required for generating the schema view")
                                                (generate-datomic-schema-view options))
        "import-logs-into-datomic"          (if (or (string/blank? (:log-dir options)) 
                                                    (string/blank? (:url options)))
                                                (println "Options log-dir and url are required for importing logs into datomic")
                                                (import-logs-into-datomic options))
        "import-helper-log-into-datomic"    (if (or (string/blank? (:log-dir options)) 
                                                    (string/blank? (:url options)))
                                                (println "Options log-dir and url are required for importing logs into datomic")
                                                (import-helper-log-into-datomic options))
        "sort-datomic-log"                  (if (string/blank? (:log-dir options))
                                                (println "Options log-dir is required for sorting the datomic log")
                                                (sort-datomic-log options))
        "excise-tmp-data"                   (if (string/blank? (:url options))
                                                (println "Option url is required for removing tmp data")
                                                (excise-tmp-data options))
        "test-datomic-data"                 (if (string/blank? (:url options))
                                                (println "Option url is require for performing tests on datomic data")
                                                (test-datomic-data options))
        "all-import-actions"                (if (or (string/blank? (:url options)) 
                                                    (string/blank? (:log-dir options)) 
                                                    (string/blank? (:acedump-dir options)) 
                                                    (string/blank? (:schema-filename options)) 
                                                    (string/blank? (:model options)))
                                                 (println "All options are required if you would like to peform all actions" options)
                                                 (all-import-actions options))
        "delete-database"                   (if (string/blank? (:url options))
                                                 (println "The url option is required for deleting a Datomic database")
                                                 (delete-database options))
        "list-databases"                    (if (string/blank? (:url options))
                                                 (println "The url to the database is required to get the list of Dataomic database names")
                                                 (list-databases options))
        "backup-database"                   (if (or (string/blank? (:url options))
                                                    (string/blank? (:backup-file options)))
                                                 (println "The options url and backup-file are required for backing up the database")
                                                 (backup-database options))
        "generate-datomic-database-report"  (if (or (string/blank? (:url options))
                                                    (string/blank? (:datomic-database-report-filename options)))
                                                 (println "The options url and datomic-database-summary-filename are required for generating the report")
                                                 (generate-datomic-database-report options))
        (exit 1 (usage summary)))))
