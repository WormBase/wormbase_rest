(ns rest-api.classes.protein.widgets.motif-details
  (:require
   [datomic.api :as d]
   [clojure.string :as str]
   [rest-api.classes.protein.core :as protein-core]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn motif-details [p]
 {:data (protein-core/get-motif-details p)
  :description "The motif details of the protein"})

(def widget
  {:name generic/name-field
   :motif_details motif-details})
