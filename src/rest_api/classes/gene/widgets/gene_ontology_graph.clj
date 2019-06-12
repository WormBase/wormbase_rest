(ns rest-api.classes.gene.widgets.gene-ontology-graph
  (:require [rest-api.classes.generic-fields :as generic]))

(defn gene-ontology-graph [gene]
  {:data (:gene/id gene)
   :description "The Gene Ontology Graph of the gene"})

(def widget
  {:gene_ontology_graph gene-ontology-graph
   :name generic/name-field})
