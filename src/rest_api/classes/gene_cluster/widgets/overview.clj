(ns rest-api.classes.gene-cluster.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn title [gc]
  {:data nil ; Appears to be a title field in acedb schema but not datomic for gene_cluster
   :description (str "The name and WormBase internal ID of " (:gene-cluster/id gc))})

(defn contains-gene [gc]
  {:data (when-let [gs (:gene-cluster/contains-gene gc)]
           (for [g gs]
             (pack-obj g)))
   :description "genes that are found in this gene cluster"})

(def widget
  {:name generic/name-field
   :title title
   :description generic/description
   :contains_gene contains-gene})
