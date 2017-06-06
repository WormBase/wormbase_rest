(ns rest-api.classes.anatomy_term.widgets.overview
  (:require
   [datomic.api :as d]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def q-anatomy-functions-involved
  '[:find ?af
    :in $ ?at
    :where [?ih :anatomy-function.involved/anatomy-term ?at]
           [?af :anatomy-function/involved ?ih]])

(def q-anatomy-function-not-involved
  '[:find ?af
    :in $ ?at
    :where [?nih :anatomy-function.not-involved/anatomy-term ?at]
           [?af :anatomy-function/not-involved ?nih]])

(def q-anatomy-term-to-expression-cluster
  '[:find ?e
    :in $ ?a
    :where [?eh :expression-cluster.anatomy-term/anatomy-term ?a]
           [?e :expression-cluster/anatomy-term ?eh]])

(defn transgenes [anatomy-term]
  {:data (when-let [expression-patterns (:anatomy-term/expr-descendent anatomy-term)]
           (filter
             some?
             (flatten
               (for [expression-pattern expression-patterns]
                 (for [tg (:expr-pattern/transgene expression-pattern)]
                   (if (contains? tg :transgene/marker-for)
                     (pack-obj tg)))))))
   :description "transgenes annotated with this anatomy_term"})

(defn expression-clusters [anatomy-term]
  (let [db (d/entity-db anatomy-term)]
    {:data (when-let [eids (d/q q-anatomy-term-to-expression-cluster
                                db (:db/id anatomy-term))]
             (for [eid eids
                   :let [expression-cluster (d/entity db (first eid))]]
               {:expession_cluster (pack-obj expression-cluster)
                :description (first (:expression-cluster/description expression-cluster))}))
     :description "expression cluster data"}))

(defn term [anatomy-term]
  {:data (when-let [holder (:anatomy-term/term anatomy-term)]
             {:id (:anatomy-term.term/text holder)
              :label (:anatomy-term.term/text holder)
              :class "anatomy_name"
              :taxonomy "all"})
   :description "Term in the Anatomy ontology"})

(defn definition [anatomy-term]
  {:data (when-let [holder (:anatomy-term/definition anatomy-term)]
           (:anatomy-term.definition/text holder))
   :description "definition of the anatomy term"})

(defn gene-ontology [anatomy-term]
  {:data (when-let [ghs (:anatomy-term/go-term anatomy-term)]
           (for [gh ghs
                 :let [go-term (:anatomy-term.go-term/go-term gh)
                       ao-code (:anatomy-term.go-term/ao-code gh)]]
            {:ao_code (pack-obj ao-code)
             :term (pack-obj go-term)}))
   :description "go_terms associated with this anatomy_term"})

(defn synonyms [anatomy-term]
  {:data (when-let [synonyms (:anatomy-term/synonym anatomy-term)]
           (for [synonym synonyms]
             (:anatomy-term.synonym/text synonym)))
   :description "synonyms that have been used to describe the anatomy term"})

(defn anatomy-function-nots [anatomy-term]
  (let [db (d/entity-db anatomy-term)]
    {:data (when-let [afids (d/q q-anatomy-function-not-involved
                                 db (:db/id anatomy-term))]
             (for [eid afids
                   :let [anatomy-function (d/entity db (first eid))]]
               {:assay (when-let [ahs (:anatomy-function/assay anatomy-function)]
                         (for [ah ahs]
                           (:ao-code/id
                             (:anatomy-function.assay/ao-code ah))))
                :bb_no_inv (when-let [hs (:anatomy-function/not-involved anatomy-function)]
                             (for [h hs]
                               {:text (pack-obj (:anatomy-function.not-involved/anatomy-term h))
                                :evidence (obj/get-evidence h)}))
                :reference (when-let [reference (:anatomy-function/reference anatomy-function)]
                             (pack-obj reference))
                :af_data (:anatomy-function/id anatomy-function)
                :phenotype (when-let [phenotype (:anatomy-function.phenotype/phenotype
                                                  (:anatomy-function/phenotype anatomy-function))]
                             (pack-obj phenotype))
                :gene (when-let [gene (:anatomy-function.gene/gene
                                        (:anatomy-function/gene anatomy-function))]
                        (pack-obj gene))
                :bp_inv (when-let [hs (:anatomy-function/involved anatomy-function)]
                          (for [h hs]
                            {:text (pack-obj (:anatomy-function.involved/anatomy-term h))
                             :evidence (obj/get-evidence h)}))}))
     :description "anatomy_functions associatated with this anatomy_term"}))

(defn wormatlas [anatomy-term]
  {:data (when-let [dbs (:anatomy-term/database anatomy-term)]
           (when-let [data (remove
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
  {:data (when-let [expression-patterns (:anatomy-term/expr-descendent anatomy-term)]
           (for [expression-pattern expression-patterns]
             {:expression_pattern
              (pack-obj expression-pattern)

              :gene
              (when-let [holders (:expr-pattern/gene expression-pattern)]
                (pack-obj (:expr-pattern.gene/gene (first holders))))

              :keys
              (keys expression-pattern)

              :reference
              (when-let [holder (:expr-pattern/reference expression-pattern)]
                (:paper/id
                  (:expr-pattern.reference/paper (first holder))))

              :certainty ; Couldn't find an example and the Perl code looks difficult to mimic. Would need context.
              nil

              :author
              (when-let [authors (:expr-pattern/author expression-pattern)]
               (:author/id  (last authors)))

              :description
              (when-let [pattern (:expr-pattern/pattern expression-pattern)]
                (first pattern))}))
   :description (str "expression patterns associated with the Anatomy_term: " (:anatomy-term/id anatomy-term))})

; Commented out widgets do not get used by the template. This functionality should be discussed and possibly be used in another widget(s)
(def widget
  {:name generic/name-field
   ;:transgenes transgenes
;   :expression_clusters expression-clusters
   :term term
   :definition definition
;   :gene_ontology gene-ontology
   :synonyms synonyms
;   :anatomy_function_nots anatomy-function-nots
;   :anatomy_functions anatomy-functions
;   :expression_patterns expression-patterns
   :wormatlas wormatlas})
