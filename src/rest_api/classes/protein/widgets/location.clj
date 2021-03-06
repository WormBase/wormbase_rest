(ns rest-api.classes.protein.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [protein]
  {:data ["GENES"
          "PROTEIN_MOTIFS"]
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [protein]
  {:data "Curated_Genes,Protein motifs"
   :description "tracks displayed in JBrowse"})

(defn genomic-image [protein]
  {:data (first (:data (generic/genomic-position protein)))
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
