(ns rest-api.classes.phenotype.widgets.transgene
  (:require
    [clojure.string :as str]
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]))

(def widget
  {:transgene nil
   :transgene_not nil
   :name nil})
