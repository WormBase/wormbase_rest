(ns rest-api.classes.gene.widgets.sequences
  (:require
   [rest-api.classes.generic-fields :as generic]))

(def widget
  {:name        generic/name-field
   :gene_models generic/corresponding-all})
