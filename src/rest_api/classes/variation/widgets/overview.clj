(ns rest-api.classes.variation.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn corresponding-gene [v]
  {:data nil
   :description "gene in which this variation is found (if any)"})

(defn evidence [v]
  {:data nil
   :description "Evidence for this Variation"})

(defn variation-type [v]
  {:data nil
   :description "the general type of the variation"})


(def widget
  {:name generic/name-field
   :status generic/status
   :taxonomy generic/taxonomy
   :corresponding_gene corresponding-gene
   :evidence evidence
   :variation_type variation-type
   :remarks generic/remarks
   :other_names generic/other-names})
