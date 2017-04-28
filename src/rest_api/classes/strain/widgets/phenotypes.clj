(ns rest-api.classes.strain.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]))

(def widgets
  {:phenotypes_not_observed nil
   :name nil
   :phenotypes nil})
