(ns rest-api.classes.gene.widgets.expression
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [rest-api.classes.gene.generic :as generic]))

(defn- expr-pattern-detail [expr-pattern qualifier]
  (pace-utils/vmap
   :Paper (if-let [paper-holders (:expr-pattern/reference expr-pattern)]
            (->> paper-holders
                 (map :expr-pattern.reference/paper)
                 (map pack-obj)))
   :Expressed_in (->> (:qualifier/anatomy-term qualifier)
                      (map pack-obj)
                      (seq))
   :Expressed_during (->> (:qualifier/life-stage qualifier)
                          (map pack-obj)
                          (seq))

   ))

(defn- expression-table-row [entity entity-name expr-pattern qualifier]
  {(keyword entity-name) (pack-obj entity)
   :evidence {:text (pack-obj expr-pattern)
              :evidence (expr-pattern-detail expr-pattern qualifier)}
   :image ""})

(defn expressed-in [gene]
  (let [db (d/entity-db gene)]
    {:data
     (if-let [anatomy-relations
              (d/q '[:find ?at ?ep ?ah
                     :in $ ?gene
                     :where
                     [?gh :expr-pattern.gene/gene ?gene]
                     [?ep :expr-pattern/gene ?gh]
                     [?ep :expr-pattern/anatomy-term ?ah]
                     [?ah :expr-pattern.anatomy-term/anatomy-term ?at]]
                   db (:db/id gene))]
       (map (fn [[anatomy expr-pattern qualifier]]
              (expression-table-row (d/entity db anatomy)
                                    "anatomy"
                                    (d/entity db expr-pattern)
                                    (d/entity db qualifier)))
            anatomy-relations))
     :description "the tissue that the gene is expressed in"}))

(def widget
  {:name generic/name-field
   :expressed-in expressed-in
   }
  )
