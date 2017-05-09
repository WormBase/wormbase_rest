(ns rest-api.classes.wbprocess.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.phenotype.core :as phenotype-core]
    [rest-api.classes.generic :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(def q-wbprocess-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?wbp
    :where [?wbp :wbprocess/phenotype ?ph]
           [?ph :wbprocess.phenotype/phenotype ?pheno]])

(defn- phenotype-table-entity
  [db pheno pato-key entity pid phenos]
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
                 wbprocess (:wbprocess/_phenotype holder)
                 pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                 wbprocess-pato-key (first pato-keys)]]
       (if (= pato-key wbprocess-pato-key)
         {:text (pack-obj wbprocess)
          :evidence (phenotype-core/var-evidence holder wbprocess pheno)})))})

(defn- phenotype-table [db wbprocess]
  (let [phenos (into {} (d/q q-wbprocess-pheno
                             db wbprocess))]
    (->>
      (flatten
        (for [pid (keys phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      phenos)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      phenos)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        phenos))))))
      (into []))))

(defn phenotypes-observed [wbprocess]
  (let [data (phenotype-table (d/entity-db wbprocess) (:db/id wbprocess))]
    {:data data
     :description "phenotypes annotated with this term"}))

(def widget
  {:phenotypes phenotypes-observed
   :name generic/name-field})
