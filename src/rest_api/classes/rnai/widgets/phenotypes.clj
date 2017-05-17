(ns rest-api.classes.rnai.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.paper.core :as paper-core]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.classes.generic :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(def q-rnai-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?var
    :where [?var :rnai/phenotype ?ph]
           [?ph :rnai.phenotype/phenotype ?pheno]])

(def q-rnai-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?var
    :where [?var :rnai/phenotype-not-observed ?ph]
           [?ph :rnai.phenotype-not-observed/phenotype ?pheno]])

(defn- phenotype-table-entity
  [db pheno pato-key entity pid phenos not-observed?]
  {:entity entity
   :phenotype {:class "phenotype"
               :id (:phenotype/id pheno)
               :label (:phenotype.primary-name/text
                        (:phenotype/primary-name pheno))
               :taxonomy "all"}
   :evidence
   (if-let [tp (seq (phenos pid))]
     (for [t tp
           :let [holder (d/entity db t)
                 rnai (if not-observed?
                             (:rnai/_phenotype-not-observed holder)
                             (:rnai/_phenotype holder))
                 pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                 rnai-pato-key (first pato-keys)]]
       (if (= pato-key rnai-pato-key)
         {:text (pack-obj rnai)
          :evidence 
          (merge
            (pace-utils/vmap
              :Genotype
              (:rnai/genotype rnai)

              :Strain
              (:strain/id  (:rnai/strain rnai))

              :paper
              (let  [paper-ref  (:rnai/reference rnai)]
                (if-let  [paper  (:rnai.reference/paper paper-ref)]
                  (paper-core/evidence paper))))
            (phenotype-core/get-evidence holder rnai pheno))})))})

(defn- phenotype-table [db rnai not-observed?]
  (let [rnai-phenos (into {} (d/q (if not-observed?
                                         q-rnai-not-pheno
                                         q-rnai-pheno)
                                       db rnai))]
    (->>
      (flatten
        (for [pid (keys rnai-phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      rnai-phenos)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      rnai-phenos
                                      not-observed?
                                      )
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        rnai-phenos
                                        not-observed?))))))
      (into []))))

(defn phenotypes-not-observed [rnai]
  (let [data (phenotype-table (d/entity-db rnai) (:db/id rnai) true)]
    {:data data
     :description "phenotypes NOT observed or associated with this object"}))

(defn phenotypes-observed [rnai]
  (let [data (phenotype-table (d/entity-db rnai) (:db/id rnai) false)]
    {:data data
     :description "phenotypes annotated with this term"}))

(def widget
  {:phenotypes_not_observed phenotypes-not-observed
   :phenotypes phenotypes-observed
   :name generic/name-field})
