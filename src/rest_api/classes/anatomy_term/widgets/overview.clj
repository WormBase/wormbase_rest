(ns rest-api.classes.anatomy_term.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn transgenes [anatomy-term]
  {:data (if-let [expression-patterns (:anatomy-term/expr-descendent anatomy-term)]
           (filter
             some?
             (flatten
               (for [expression-pattern expression-patterns]
                 (for [tg (:expr-pattern/transgene expression-pattern)]
                   (if (contains? tg :transgene/marker-for)
                     (pack-obj tg)))))))
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
  {:data (if-let [synonyms (:anatomy-term/synonym anatomy-term)]
           (for [synonym synonyms]
             (:anatomy-term.synonym/text synonym)))
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
  {:data (:db/id anatomy-term)
   :description "anatomy_functions associatated with this anatomy_term"})

(defn expression-patterns [anatomy-term]
  {:data (if-let [expression-patterns (:anatomy-term/expr-descendent anatomy-term)]
           (for [expression-pattern expression-patterns]
             {:expression_pattern
              (pack-obj expression-pattern)

              :gene
              (if-let [holders (:expr-pattern/gene expression-pattern)]
                (pack-obj (:expr-pattern.gene/gene (first holders))))

              :keys
              (keys expression-pattern)

              :reference
              (if-let [holder (:expr-pattern/reference expression-pattern)]
                (:paper/id
                  (:expr-pattern.reference/paper (first holder))))

              :certainty ; Couldn't find an example and the Perl code looks difficult to mimic. Would need context.
              nil

              :author
              (if-let [authors (:expr-pattern/author expression-pattern)]
               (:author/id  (last authors)))

              :description
              (if-let [pattern (:expr-pattern/pattern expression-pattern)]
                (first pattern))
              }))
   :description (str "expression patterns associated with the Anatomy_term: " (:anatomy-term/id anatomy-term))})

(def widget
  {:transgenes transgenes ; Doesn't appear to get used by UI code. Also the perl code was close but not quite correct.
   :expression_clusters expression-clusters
   :term term
   :name generic/name-field
   :definition definition
   :gene_ontology gene-ontology
   :synonyms synonyms
   :anatomy_function_nots anatomy-function-nots ; Example: WBbt:0006989; can't find link to anatomy function which is what is in perl code
   :anatomy_functions anatomy-functions ; can't find link to anatomy-term
   :expression_patterns expression-patterns ; Doesn't seem to get used by the widget
   :wormatlas wormatlas
   })
