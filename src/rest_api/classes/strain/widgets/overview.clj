(ns rest-api.classes.strain.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn mutagen [s]
  {:data nil
   :description "the mutagen used to generate this stain"})

(defn outcrossed [s]
  {:data nil
   :description "extent to which the strain has been outcrossed"})

(defn genotype [s]
  {:data nil
   :description "the genotype of the strain"})

(defn other-names [s]
  {:data nil
   :description (str "other names that have been used to refer to " (:strain/id s))})

(def widget
  {:name generic/name-field
   :mutagen mutagen
   :outcrossed outcrossed
   :taxonomy generic/taxonomy
   :genotype genotype
   :remarks generic/remarks
   :other_names other-names})
