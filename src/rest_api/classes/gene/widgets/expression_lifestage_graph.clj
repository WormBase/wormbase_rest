(ns rest-api.classes.gene.widgets.expression-lifestage-graph
  (:require [rest-api.classes.generic-fields :as generic]))

(defn expression-lifestage-graph [gene]
  {:data (:gene/id gene)
   :description "The Expression-life stage Graph of the gene"})

(def widget
  {:expression_lifestage_graph expression-lifestage-graph
   :name generic/name-field})
