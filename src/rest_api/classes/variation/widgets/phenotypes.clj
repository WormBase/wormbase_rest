(ns rest-api.classes.variation.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]))

(def widget
  {:phenotypes_not_observed nil
   :name nil
   :phenotpes nil})
