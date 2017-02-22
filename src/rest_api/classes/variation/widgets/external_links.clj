(ns rest-api.classes.variation.widgets.external-links
  (:require
    [rest-api.classes.generic :as generic]
    [rest-api.classes.variation.generic :as variation-generic]))

(defn xrefs [variation]
  (generic/xrefs "variation" variation))

(def widget
  {:name  variation-generic/name-field
   :xrefs xrefs})
