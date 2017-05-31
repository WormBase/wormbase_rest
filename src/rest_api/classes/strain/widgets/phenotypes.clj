(ns rest-api.classes.strain.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def q-strain-pheno
  '[:find ?pheno  (distinct ?ph)
    :in $ ?s
    :where [?s :strain/phenotype ?ph]
           [?ph :strain.phenotype/phenotype ?pheno]])

(def q-strain-not-pheno
  '[:find ?pheno  (distinct ?ph)
    :in $ ?s
    :where [?s :strain/phenotype-not-observed ?ph]
    [?ph :strain.phenotype-not-observed/phenotype ?pheno]])

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
                 strain  (if not-observed?
                           (:strain/_phenotype-not-observed holder)
                           (:strain/_phenotype holder))
                 pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                 strain-pato-key (first pato-keys)]]
       (if (= pato-key strain-pato-key)
         {:text (pack-obj strain)
          :evidence (phenotype-core/get-evidence holder strain pheno)})))})

(defn- phenotype-table [db strain not-observed?]
  (let [strain-phenos (into {} (d/q (if not-observed?
                                      q-strain-not-pheno
                                      q-strain-pheno)
                                    db strain))]
    (->>
      (flatten
        (for [pid (keys strain-phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      strain-phenos)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      strain-phenos
                                      not-observed?
                                      )
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        strain-phenos
                                        not-observed?))))))
      (into []))))

(defn phenotypes-not-observed [strain]
  (let  [data  (phenotype-table  (d/entity-db strain)  (:db/id strain) true)]
    {:data data
     :description "phenotypes NOT observed or associated with this object"}))

(defn phenotypes-observed [strain]
  (let  [data  (phenotype-table  (d/entity-db strain)  (:db/id strain) false)]
    {:data data
     :description "phenotypes annotated with this term"}))

(def widget
  {:phenotypes_not_observed phenotypes-not-observed
   :phenotypes phenotypes-observed
   :name generic/name-field})
