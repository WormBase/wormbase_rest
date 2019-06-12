(ns rest-api.classes.gene.widgets.human-disease-graph
  (:require [rest-api.classes.generic-fields :as generic]))

(defn human-disease-graph [gene]
  {:data (:gene/id gene)
   :description "The Human Disease Graph of the gene"})

(def widget
  {:human_disease_graph human-disease-graph
   :name generic/name-field})
