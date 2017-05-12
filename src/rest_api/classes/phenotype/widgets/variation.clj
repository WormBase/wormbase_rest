(ns rest-api.classes.phenotype.widgets.variation
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic :as generic]))

(def widget
  {:varaition nil
   :variation_not nil
   :name generic/name-field})
