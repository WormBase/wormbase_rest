(ns rest-api.classes.variation.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn status [v]
  {:data nil
   :description (str "current status of the Variation:" (:variation/id v) " if not Live or Valid")})

(defn corresponding-gene [v]
  {:data nil
   :description "gene in which this variation is found (if any)"})

(defn evidence [v]
  {:data nil
   :description "Evidence for this Variation"})

(defn variation-type [v]
  {:data nil
   :description "the general type of the variation"})

(defn other-names [v]
  {:data nil
   :description (str "other names that have been used to refer to " (:variation/id v))})

(def widget
  {:name generic/name-field
   :status status
   :taxonomy generic/taxonomy
   :corresponding_gene corresponding-gene
   :evidence evidence
   :variation_type variation-type
   :remarks generic/remarks
   :other_names other-names})
