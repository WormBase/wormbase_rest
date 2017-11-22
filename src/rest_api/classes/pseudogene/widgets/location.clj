(ns rest-api.classes.pseudogene.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [pseudogene]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:pseudogene/species pseudogene)))
             ["GENES"])
   :description "tracks displayed in GBrowse"})

(defn genomic-image [pseudogene]
  {:data (sequence-fns/genomic-obj pseudogene)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
