(ns rest-api.classes.gene.widgets.expression-anatomy-graph
  (:require [rest-api.classes.generic-fields :as generic]))

(defn expression-anatomy-graph [gene]
  {:data (:gene/id gene)
   :description "The Expression-anatomy Graph of the gene"})

(def widget
  {:expression_anatomy_graph expression-anatomy-graph
   :name generic/name-field})
