(ns rest-api.classes.variation.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic :as generic]))

(def widget
  {:phenotypes_not_observed nil
   :name generic/name-field
   :phenotpes nil})
