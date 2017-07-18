(ns rest-api.classes.gene.widgets.phenotype-graph
  (:require [rest-api.classes.generic-fields :as generic]))

(defn phenotype-graph [gene]
  {:data (:gene/id gene)
   :descriptions "The Phenotype Graph of the gene"})

(def widget
  {:phenotype_graph phenotype-graph
   :name generic/name-field})
