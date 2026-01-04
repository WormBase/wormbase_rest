(ns rest-api.perf.config
  "Configuration for response size analysis."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-conn]]))

(def default-thresholds
  {:large-response-kb 500
   :warning-response-kb 100
   :deep-nesting-level 10
   :high-entity-count 100})

(defn load-config
  "Load config from EDN file, returns nil if not found."
  [path]
  (try
    (when (.exists (io/file path))
      (-> path slurp edn/read-string))
    (catch Exception e
      (println "Warning: Could not load config:" (.getMessage e))
      nil)))

(defn auto-discover-entities
  "Find N sample entity IDs from the database."
  [entity-ns n]
  (try
    (let [db (d/db datomic-conn)
          id-attr (keyword entity-ns "id")]
      (->> (d/q '[:find [?id ...]
                  :in $ ?attr
                  :where [_ ?attr ?id]]
                db id-attr)
           (take n)
           (vec)))
    (catch Exception _
      [])))

(defn get-entity-ids
  "Get entity IDs from config or auto-discover."
  [entity-ns config sample-count]
  (or (get-in config [:entity-ids entity-ns])
      (auto-discover-entities entity-ns sample-count)))

(def all-entity-namespaces
  ["analysis" "anatomy-term" "antibody" "cds" "clone" "construct"
   "do-term" "expr-pattern" "expr-profile" "expression-cluster"
   "feature" "gene" "gene-class" "gene-cluster" "genotype" "go-term"
   "homology-group" "interaction" "laboratory" "life-stage"
   "microarray-results" "molecule" "motif" "operon" "paper"
   "pcr-oligo" "person" "phenotype" "picture" "position-matrix"
   "protein" "pseudogene" "rearrangement" "rnai" "sequence" "strain"
   "structure-data" "transcript" "transgene" "transposon"
   "transposon-family" "variation" "wbprocess"])
