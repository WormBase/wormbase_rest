(ns rest-api.classes.variation.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.classes.generic :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(def q-variation-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?var
    :where [?var :variation/phenotype ?ph]
           [?ph :variation.phenotype/phenotype ?pheno]])

(def q-variation-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?var
    :where [?var :variation/phenotype-not-observed ?ph]
           [?ph :variation.phenotype-not-observed/phenotype ?pheno]])

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
                 variation (if not-observed?
                             (:variation/_phenotype-not-observed holder)
                             (:variation/_phenotype holder))
                 pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                 variation-pato-key (first pato-keys)]]
       (if (= pato-key variation-pato-key)
         {:text (pack-obj variation)
          :evidence (phenotype-core/var-evidence holder variation pheno)})))})

(defn- phenotype-table [db variation not-observed?]
  (let [variation-phenos (into {} (d/q (if not-observed?
                                         q-variation-not-pheno
                                         q-variation-pheno)
                                       db variation))]
    (->>
      (flatten
        (for [pid (keys variation-phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      variation-phenos)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      variation-phenos
                                      not-observed?
                                      )
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        variation-phenos
                                        not-observed?))))))
      (into []))))

(defn phenotypes-not-observed [variation]
  (let [data (phenotype-table (d/entity-db variation) (:db/id variation) true)]
    {:data data
     :description "phenotypes NOT observed or associated with this object"}))

(defn phenotypes-observed [variation]
  (let [data (phenotype-table (d/entity-db variation) (:db/id variation) false)]
    {:data data
     :description "phenotypes annotated with this term"}))

(def widget
  {:phenotypes_not_observed phenotypes-not-observed
   :phenotypes phenotypes-observed
   :name generic/name-field})
