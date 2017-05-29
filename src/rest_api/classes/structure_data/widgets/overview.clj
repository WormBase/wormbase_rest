(ns rest-api.classes.structure-data.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-field [s]
  {:data nil
   :description "sequence of structure"})

(defn protein-homology [s]
  {:data nil
   :description "Protein homologs for this structure"})

(defn status [s]
  {:data nil
   :description (str "current status of the Structure_data:" (:structure-data/id s) " if not Live or Valid")})

(defn homology-data [s]
  {:data nil
   :description "homology data re: this structure"})

(def widget
  {:name generic/name-field
   :sequence sequence-field
   :protein_homology protein-homology
   :status status
   :homology_data homology-data
   :remarks generic/remarks})
