(ns rest-api.classes.clone.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [clone]
  {:data ["GENES"
          "CLONES"
          "LINKS_AND_SUPERLINKS"
          "GENOMIC_CANONICAL"]
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [clone]
  {:data "Curated_Genes,YACs_Fosmids_Cosmids,Contig submissions,Links and Superlinks"
   :description "tracks displayed in JBrowse"})

(defn genomic-image [clone]
  {:data (sequence-fns/genomic-obj clone)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
