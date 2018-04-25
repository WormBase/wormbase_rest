(ns rest-api.classes.gene-cluster.widgets.overview
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn contains-genes [gc]
  {:data (some->> (:gene-cluster/contains-gene gc)
                  (map pack-obj)
                  (sort generic-functions/compare-gene-name))
   :description "Genes that are found in this gene cluster"})

(def widget
  {:name generic/name-field
   :description generic/description
   :contains_genes contains-genes})
