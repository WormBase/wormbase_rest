(ns rest-api.classes.gene.widgets.ontology
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.gene.generic :as generic]
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
        (if (:go-annotation/go-term-relation anno)
          (concat
           (for [{rel :go-annotation.go-term-relation/text
                  gt :go-annotation.go-term-relation/go-term}
                 (:go-annotation/go-term-relation anno)]
             {:class "Gene Ontology Consortium"
              :dbt "GO_REF"
              :id (:go-term/id gt)
              :label (:go-term/id gt)})))
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

(def widget
  {:name                   generic/name-field
   :gene_ontology_summary  gene-ontology-summary
   :gene_ontology          gene-ontology-full})
