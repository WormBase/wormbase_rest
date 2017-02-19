(ns rest-api.classes.gene.widgets.external-links
  (:require
    [rest-api.classes.generic :as generic]
    [rest-api.classes.gene.generic :as gene-generic]))

(defn xrefs [gene]
  (generic/xrefs "gene" gene))

(def widget
  {:name  gene-generic/name-field
   :xrefs xrefs})
