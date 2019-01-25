(ns rest-api.classes.strain.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.strain.core :refer [get-genotype]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn mutagen [s]
  {:data (first (:strain/mutagen s))
   :description "the mutagen used to generate this stain"})

(defn outcrossed [s]
  {:data (:strain/outcrossed s)
   :description "extent to which the strain has been outcrossed"})

(defn genotype [s]
  {:data (get-genotype s)
   :description "the genotype of the strain"})

(def widget
  {:name generic/name-field
   :mutagen mutagen
   :outcrossed outcrossed
   :taxonomy generic/taxonomy
   :genotype genotype
   :remarks generic/remarks
   :other_names generic/other-names})
