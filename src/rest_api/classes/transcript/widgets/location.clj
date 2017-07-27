(ns rest-api.classes.transcript.widgets.location
  (:require
    [rest-api.classes.sequence.main :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [transcript]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:transcript/species transcript)))
           ["GENES"
            "EST_BEST"])
   :description "tracks displayed in GBrowse"})

(defn genomic-image [transcript]
  {:data (sequence-fns/genomic-obj transcript)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})