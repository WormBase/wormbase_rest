(ns rest-api.classes.pseudogene.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [pseudogene]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:pseudogene/species pseudogene)))
             ["GENES"])
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [pseudogene]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:pseudogene/species pseudogene)))
             "Curated_Genes")
   :description "tracks displayed in JBrowse"})

(defn genomic-image [pseudogene]
  {:data (sequence-fns/genomic-obj pseudogene)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
