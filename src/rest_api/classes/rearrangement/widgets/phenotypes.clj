(ns rest-api.classes.rearrangement.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def q-rearrangement-pheno
  '[:find ?pheno  (distinct ?ph)
    :in $ ?r
    :where [?r :rearrangement/phenotype ?ph]
           [?ph :rearrangement.phenotype/phenotype ?pheno]])

(def q-rearrangement-not-pheno
  '[:find ?pheno  (distinct ?ph)
    :in $ ?r
    :where [?r :rearrangement/phenotype-not-observed ?ph]
    [?ph :rearrangement.phenotype-not-observed/phenotype ?pheno]])

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
                 rearrangement (if not-observed?
                                 (:rearrangement/_phenotype_not_observed holder)
                                 (:rearrangement/_phenotype holder))
                 pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                 rearrangement-pato-key (first pato-keys)]]
       (if (= pato-key rearrangement-pato-key)
         {:text (pack-obj rearrangement)
          :evidence (phenotype-core/get-evidence holder rearrangement pheno)})))})

(defn- phenotype-table [db rearrangement not-observed?]
  (let [rearrangement-phenos (into {} (d/q (if not-observed?
                                             q-rearrangement-not-pheno
                                             q-rearrangement-pheno)
                                           db rearrangement))]
    (->>
      (flatten
        (for [pid (keys rearrangement-phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      rearrangement-phenos)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      rearrangement-phenos
                                      not-observed?)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        rearrangement-phenos
                                        not-observed?))))))
      (into []))))

(defn phenotypes-not-observed [rearrangement]
  (let [data (phenotype-table (d/entity-db rearrangement) (:db/id rearrangement) true)]
    {:data data
     :description "phenotypes NOT observed or associated with this object"}))

(defn phenotypes-observed [rearrangement]
  (let [data (phenotype-table (d/entity-db rearrangement) (:db/id rearrangement) false)]
    {:data data
     :description "phenotypes annotated with this term"}))

(def widget
  {:phenotypes_not_observed phenotypes-not-observed
   :phenotypes phenotypes-observed
   :name generic/name-field})
