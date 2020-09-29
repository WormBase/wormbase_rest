(ns rest-api.classes.genotype.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))


(defn synonym [genotype]
  {:data (sort (:genotype/genotype-synonym genotype))
   :description "a synonym for the genotype"})


(def widget
  {:name generic/name-field
   :synonym synonym
   })
