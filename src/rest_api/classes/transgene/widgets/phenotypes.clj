(ns rest-api.classes.transgene.widgets.phenotypes
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]))

(def widget
  {:phenotypes_not_observed nil
   :phenotypes nil
   :name nil})
