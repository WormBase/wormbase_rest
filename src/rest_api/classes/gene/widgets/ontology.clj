(ns rest-api.classes.gene.widgets.ontology
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def ^:private division-names
  {:go-term.type/molecular-function "Molecular_function"
   :go-term.type/cellular-component "Cellular_component"
   :go-term.type/biological-process "Biological_process"})

(defn- go-anno-extension-fn [relation-type-tag target-tag]
  (fn [relation]
    [(relation-type-tag relation)
     (pack-obj (target-tag relation))]))

(defn- go-anno-extensions [anno]
  (->>
   (concat
    (map (go-anno-extension-fn
          :go-annotation.molecule-relation/text
          :go-annotation.molecule-relation/molecule)
         (:go-annotation/molecule-relation anno))
    (map (go-anno-extension-fn
          :go-annotation.anatomy-relation/text
          :go-annotation.anatomy-relation/anatomy-term)
         (:go-annotation/anatomy-relation anno))
    (map (go-anno-extension-fn
          :go-annotation.gene-relation/text
          :go-annotation.gene-relation/gene)
         (:go-annotation/gene-relation anno))
    (map (go-anno-extension-fn
          :go-annotation.life-stage-relation/text
          :go-annotation.life-stage-relation/life-stage)
         (:go-annotation/life-stage-relation anno))
    (map (go-anno-extension-fn
          :go-annotation.go-term-relation/text
          :go-annotation.go-term-relation/go-term)
         (:go-annotation/go-term-relation anno)))
   (map (fn [[relation_type target]]
          {relation_type #{target}}))))


(defn- go-anno-xref [anno-db]
  (let [db-id (:database/id (:go-annotation.database/database anno-db))
        obj-id (:go-annotation.database/text anno-db)
        db-field (:go-annotation.database/database-field anno-db)]
    {:id obj-id
     :class db-id
     :dbt (:database-field/id db-field)
     :label (str/join ":" [db-id obj-id])}))

(defn- term-summary-table [db annos]
  (->> (group-by :term annos)
       (map
        (fn [[term term-annos]]
          (let [exts (->> (map :anno term-annos)
                          (map go-anno-extensions)
                          (apply concat)
                          (apply (partial merge-with set/union)))]
            {:extensions exts
             :term_id (pack-obj "go-term" term :label (:go-term/id term))
             :term_description (if (not-empty exts)
                                 [(pack-obj term) {:evidence exts}]
                                 [(pack-obj term)])})))))

(defn- term-table-full [db annos]
  (map
   (fn [{:keys [term code anno]}]
     {:anno_id (:go-annotation/id anno)
      :with
      (seq
       (concat
        (map pack-obj (:go-annotation/interacting-gene anno))
        (map pack-obj (:go-annotation/inferred-from-go-term anno))
        (map pack-obj (:go-annotation/motif anno))
        (map pack-obj (:go-annotation/rnai-result anno))
        (map pack-obj (:go-annotation/variation anno))
        (map pack-obj (:go-annotation/phenotype anno))
        (map go-anno-xref (:go-annotation/database anno))))

      :evidence_code
      {:text (:go-code/id code)

       :evidence
       (pace-utils/vmap
        :Date_last_updated
        (if-let [d (:go-annotation/date-last-updated anno)]
          [{:class "text"
            :id (date/format-date3 (str d))
            :label (date/format-date3 (str d))}])

        :Contributed_by
        (if-let [cb (:go-annotation/contributed-by anno)]
          (pack-obj cb))

        :Reference
        (if (:go-annotation/reference anno)
          [(pack-obj "paper"
                     (:go-annotation/reference anno))])

        :GO_reference
        (seq (for [{go-id :go-annotation.go-reference/text
                    ref-db :go-annotation.go-reference/database
                    ref-db-field :go-annotation.go-reference/database-field}
                   (:go-annotation/go-reference anno)]
               {:class (:database/name ref-db)
                :dbt (:database-field/id ref-db-field)
                :id go-id
                :label go-id}))

        )}

      :go_type
      (if-let [go-type (:go-term/type term)]
        (division-names go-type))

      :term (if-let [extensions (->> (go-anno-extensions anno)
                                     (apply (partial merge-with concat)))]
              {:evidence extensions})

      :term_id
      (pack-obj "go-term" term :label (:go-term/id term))

      :term_description
      (pack-obj "go-term" term)}
     )
   annos))

(defn gene-ontology-full [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->>
      (d/q '[:find ?div ?term ?code ?anno
             :in $ ?gene
             :where [?anno :go-annotation/gene ?gene]
             [?anno :go-annotation/go-term ?term]
             [?anno :go-annotation/go-code ?code]
             [?term :go-term/type ?tdiv]
             [?tdiv :db/ident ?div]]
           db (:db/id gene))
       (map
         (fn [[div term code anno]]
           {:division div
            :term     (d/entity db term)
            :code     (d/entity db code)
            :anno     (d/entity db anno)}))
       (group-by :division)
       (map
         (fn [[key annos]]
           [(division-names key)
            (term-table-full db annos)]))
       (into {}))

     :description
     "gene ontology associations"}))

(defn gene-ontology-summary [gene]
 (let [db (d/entity-db gene)]
  {:data
   (->>
    (d/q '[:find ?div ?term ?anno
           :in $ ?gene
           :where
           [?anno :go-annotation/gene ?gene]
           [?anno :go-annotation/go-term ?term]
           [?term :go-term/type ?tdiv]
           [?tdiv :db/ident ?div]]
         db (:db/id gene))
    (map
     (fn [[div term anno]]
       {:division div
        :term (d/entity db term)
        :anno (d/entity db anno)}))
    (group-by :division)
    (map
     (fn [[key annos]]
       [(division-names key)
        (term-summary-table db annos)]))
    (into {}))

   :description
   "gene ontology associations"}))

(def slim
  (->>
   ["GO:0003824" ;molecular function
    ;; "GO:0004872"
    "GO:0005102"
    "GO:0005215"
    "GO:0005198"
    "GO:0008092"
    "GO:0003677"
    "GO:0003723"
    ;; "GO:0001071"
    "GO:0036094"
    "GO:0046872"
    "GO:0030246"
    ;; "GO:0003674"
    "GO:0008283" ;biological process
    "GO:0071840"
    "GO:0051179"
    "GO:0032502"
    "GO:0000003"
    "GO:0002376"
    "GO:0050877"
    "GO:0050896"
    "GO:0023052"
    "GO:0010467"
    "GO:0019538"
    "GO:0006259"
    "GO:0044281"
    "GO:0050789"
    "GO:0042592"
    "GO:0007610"
    ;; "GO:0008150"
    "GO:0005576" ;cellular component
    "GO:0005737"
    "GO:0005856"
    "GO:0005739"
    "GO:0005634"
    "GO:0005694"
    "GO:0016020"
    "GO:0031982"
    "GO:0071944"
    "GO:0030054"
    "GO:0042995"
    "GO:0032991"
    "GO:0045202"
    ;; "GO:0005575"
    ]
   (map vector (repeat :go-term/id) )))

(def aspects
  (->> ["GO:0008150" "GO:0003674" "GO:0005575"]
       (map vector (repeat :go-term/id))))

(defn slim-order [slim-ref]
  (let [order (zipmap slim (iterate inc 0))
        n (count slim)]
    (or (order slim-ref)
        n)))

(defn gene-ontology-ribbon [gene]
  {:data (let [db (d/entity-db gene)
               tuples (concat
                       ; annotated with descendants of slim term
                       (d/q '[:find ?slim ?term (count ?anno)
                              :in $ ?gene [?slim ...]
                              :where
                              [?anno :go-annotation/gene ?gene]
                              [?anno :go-annotation/go-term ?term]
                              [?term :go-term/ancestor ?slim]]
                            db (:db/id gene) slim)
                       ; annotated with slim term directly
                       (d/q '[:find ?slim ?slim (count ?anno)
                              :in $ ?gene [?slim ...]
                              :where
                              [?anno :go-annotation/gene ?gene]
                              [?anno :go-annotation/go-term ?slim]]
                            db (:db/id gene) slim))
               terms (->> tuples
                          (map (comp (partial d/entid db) second))
                          (set))
               tuples-other (->> (d/q '[:find ?aspect ?term (count ?anno)
                                        :in $ ?gene [?aspect ...]
                                        :where
                                        [?anno :go-annotation/gene ?gene]
                                        [?anno :go-annotation/go-term ?term]
                                        [?term :go-term/ancestor ?aspect]]
                                      db (:db/id gene) aspects)
                                 (filter (fn [[_ term _]]
                                           (not (terms term)))))]
           (->> (concat tuples tuples-other)
                (reduce (fn [result [slim-ref term-ref anno-count]]
                          (update result slim-ref (partial cons {:term (pack-obj (d/entity db term-ref))
                                                                 :annotation_count anno-count})))
                        (zipmap (concat slim aspects) (repeat [])))
                (sort-by (fn [[slim-ref _]]
                           (slim-order slim-ref)))
                (map (fn [[slim-ref terms]]
                       {:slim (let [slim-term (d/entity db slim-ref)
                                    packed (-> (pack-obj slim-term)
                                               (assoc :definition (first (:go-term/definition slim-term))))]
                                (if ((set aspects) slim-ref)
                                  (update packed :label #(format "other %s" %))
                                  packed))
                        :aspect (obj/humanize-ident (:go-term/type (d/entity db slim-ref)))
                        :terms terms}))
                (seq)))
   :description "data for drawing the gene ontology ribbon"})

(def widget
  {:name                   generic/name-field
   :gene_ontology_summary  gene-ontology-summary
   :gene_ontology          gene-ontology-full
   :gene_ontology_ribbon   gene-ontology-ribbon})
