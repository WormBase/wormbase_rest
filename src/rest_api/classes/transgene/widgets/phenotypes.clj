(ns rest-api.classes.transgene.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.classes.generic :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def q-transgene-pheno
  '[:find ?pheno  (distinct ?ph)
    :in $ ?r
    :where [?r :transgene/phenotype ?ph]
           [?ph :transgene.phenotype/phenotype ?pheno]])

(def q-transgene-not-pheno
  '[:find ?pheno  (distinct ?ph)
    :in $ ?r
    :where [?r :transgene/phenotype-not-observed ?ph]
           [?ph :transgene.phenotype-not-observed/phenotype ?pheno]])

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
                 transgene (if not-observed?
                             (:transgene/_phenotype_not_observed holder)
                             (:transgene/_phenotype holder))
                 pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                 transgene-pato-key (first pato-keys)]]
       (if (= pato-key transgene-pato-key)
         {:text (pack-obj transgene)
          :evidence (phenotype-core/get-evidence holder transgene pheno)})))})

(defn- phenotype-table [db transgene not-observed?]
  (let [transgene-phenos (into {} (d/q (if not-observed?
                                         q-transgene-not-pheno
                                         q-transgene-pheno)
                                       db transgene))]
    (->>
      (flatten
        (for [pid (keys transgene-phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      transgene-phenos)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      transgene-phenos
                                      not-observed?)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        transgene-phenos
                                        not-observed?))))))
      (into []))))

(defn phenotypes-not-observed [transgene]
  (let [data (phenotype-table (d/entity-db transgene) (:db/id transgene) true)]
    {:data data
     :description "phenotypes NOT observed or associated with this object"}))

(defn phenotypes-observed [transgene]
  (let [data (phenotype-table (d/entity-db transgene) (:db/id transgene) false)]
    {:data data
     :description "phenotypes annotated with this term"}))

(def widget
  {:phenotypes_not_observed phenotypes-not-observed
   :phenotypes phenotypes-observed
   :name generic/name-field})
