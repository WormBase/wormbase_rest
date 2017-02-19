(ns rest-api.classes.variation.widgets.external-links
  (:require
    [rest-api.classes.generic :as generic]
    [rest-api.classes.variation.generic :as variation-generic]))

(defn xrefs [gene]
  (generic/xrefs "variation" gene))

(def widget
  {:name  variation-generic/name-field
   :xrefs xrefs})
