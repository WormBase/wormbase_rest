(ns rest-api.classes.feature.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-ontology-terms [f]
  {:data nil
   :description "sequence ontology terms describing the feature"})

(defn binds-gene-product [f]
  {:data nil
   :description "gene products that bind to the feature"})

(defn defined-by [f]
  {:data nil
   :description "how the sequence feature was defined"})

(defn transcription-factor [f]
  {:data nil
   :description "Transcription factor of the feature"})

(defn method [f]
  {:data nil
   :description "the method used to describe the Feature"})

(defn other-names [f]
  {:data nil
   :description (str "other names that have been used to refer to " (:feature/id f))})

(def widget
  {:name generic/name-field
   :sequence_ontology_terms sequence-ontology-terms
   :binds_gene_product binds-gene-product
   :taxonomgy generic/taxonomy
   :defined_by defined-by
   :description generic/description
   :transcription_factor transcription-factor
   :remarks generic/remarks
   :method method
   :other_names other-names})
