(ns rest-api.classes.operon.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [operon]
  {:data ["GENES"
          "OPERONS"]
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [operon]
  {:data "Curated_Genes,Operons"
   :description "tracks displayed in JBrowse"})

(defn genomic-image [operon]
  {:data (sequence-fns/genomic-obj operon)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
