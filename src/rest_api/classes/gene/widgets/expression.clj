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

   :expression_pattern
   (assoc (pack-obj expr-pattern)
          :curated_images
          (->> (:picture/_expr-pattern expr-pattern)
               (map pack-obj)
               (map #(assoc % :thumbnail
                            {:format "jpg",
                             :name "WBPaper00032514/F6.large_A",
                             :class "/img-static/pictures"}))
               (seq)))

   :details
   {:evidence (expr-pattern-detail expr-pattern qualifier)}
   })

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
                                    "anatomy_term"
                                    (d/entity db expr-pattern)
                                    (d/entity db qualifier)))
            anatomy-relations))
     :description "the tissue that the gene is expressed in"}))

(def widget
  {:name generic/name-field
   :expressed_in expressed-in
   }
  )
