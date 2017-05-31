(ns rest-api.classes.homology-group.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn proteins [h]
  {:data nil
   :description "proteins related to this homology_group"})

(defn title [h]
  {:data nil
   :description "title for this homology group"})

(defn type-field [h]
  {:data nil
   :description "type of homology group"})

(defn gene-ontology-terms [g]
  {:data nil
   :description "gene ontology terms associated to this homology group"})

(def widget
  {:name generic/name-field
   :proteins proteins
   :remarks generic/remarks
   :title title
   :type type-field
   :gene_ontology_terms gene-ontology-terms})
