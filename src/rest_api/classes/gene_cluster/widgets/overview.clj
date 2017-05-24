(ns rest-api.classes.gene-cluster.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn title [gc]
  {:data nil ; Appears to be a title field in acedb schema but not datomic for gene_cluster
   :description (str "The name and WormBase internal ID of " (:gene-cluster/id gc))})

(defn description [gc]
  {:data (first (:gene-cluster/description gc))
   :description (str "Description of the Gene_cluster "  (:gene-cluster/id gc))})

(defn contains-gene [gc]
  {:data (when-let [gs (:gene-cluster/contains-gene gc)]
           (for [g gs]
             (pack-obj g)))
   :description "genes that are found in this gene cluster"})

(def widget
  {:name generic/name-field
   :title title
   :description description
   :contains_gene contains-gene})
