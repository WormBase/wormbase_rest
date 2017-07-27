(ns rest-api.classes.sequence.widgets.location
  (:require
    [rest-api.classes.sequence.main :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [s]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:sequence/species s)))
           ["GENES"
            "EST_BEST"])
   :description "tracks displayed in GBrowse"})

(defn genomic-image [s]
  {:data (sequence-fns/genomic-obj s)
   :description "The genomic sequence of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})