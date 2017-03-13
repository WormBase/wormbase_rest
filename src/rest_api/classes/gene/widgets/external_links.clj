(ns rest-api.classes.gene.widgets.external-links
  (:require
    [rest-api.classes.generic :as generic]))

(defn xrefs [gene]
  (generic/xrefs gene))

(def widget
  {:name generic/name-field
   :xrefs xrefs})
