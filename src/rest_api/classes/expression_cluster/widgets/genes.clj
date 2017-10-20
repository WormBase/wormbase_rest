(ns rest-api.classes.expression-cluster.widgets.genes
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn genes [ec]
  {:data (some->> (:expression-cluster/gene ec)
                  (map :expression-cluster.gene/gene)
                  (distinct)
                  (map pack-obj)
                  (sort-by :label))
   :description (str "The name and WormBase internal ID of " (:expression-cluster/id ec))})

(def widget
  {:name generic/name-field
   :genes genes})
