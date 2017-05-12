(ns rest-api.classes.anatomy_term.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn transgenes [anatomy-term]
  {:data nil
   :description "transgenes annotated with this anatomy_term"})

(defn expression-clusters [anatomy-term]
  {:data nil
   :description "expression cluster data"})

(defn term [anatomy-term]
  {:data (if-let [holder (:anatomy-term/term anatomy-term)]
             {:id (:anatomy-term.term/text holder)
              :label (:anatomy-term.term/text holder)
              :class "anatomy_name"
              :taxonomy "all"})
   :description "Term in the Anatomy ontology"})

(defn definition [anatomy-term]
  {:data (if-let [holder (:anatomy-term/definition anatomy-term)]
           (:anatomy-term.definition/text holder))
   :description "definition of the anatomy term"})

(defn gene-ontology [anatomy-term]
  {:data nil
   :description "go_terms associated with this anatomy_term"})

(defn synonyms [anatomy-term]
  {:data nil
   :description "synonyms that have been used to describe the anatomy term"})

(defn anatomy-function-nots [anatomy-term]
  {:data nil
   :description "anatomy_functions associatated with this anatomy_term"})

(defn wormatlas [anatomy-term]
  {:data (if-let [dbs (:anatomy-term/database anatomy-term)]
           (if-let [data (remove
                           nil?
                           (for [db dbs]
                             (if (= (:database-field/id (:anatomy-term.database/field db)) "html")
                               (:anatomy-term.database/accession db))))]
             {:html {:ids data}}))
   :description "link to WormAtlas record"})

(defn anatomy-functions [anatomy-term]
  {:data nil
   :description "anatomy_functions associatated with this anatomy_term"})

(defn expression-patterns [anatomy-term]
  {:data (keys anatomy-term)
   :description (str "expression patterns associated with the Anatomy_term: " (:anatomy-term/id anatomy-term))})

(def widget
  {:transgenes transgenes
   :expression_clusters expression-clusters
   :term term
   :name generic/name-field
   :definition definition
   :gene_ontology gene-ontology
   :synonyms synonyms
   :anatomy_function_nots anatomy-function-nots
   :wormatlas wormatlas
   :anatomy_functions anatomy-functions
   :expression_patterns expression-patterns})
