(ns rest-api.classes.transposon.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [transposon]
  {:data ["TRANSPOSON_GENES"
          "TRANSPOSONS"]
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [transposon]
  {:data "Transposon Genes,Transposons"
   :description "tracks displayed in JBrowse"})

(defn genomic-image [transposon]
  {:data (sequence-fns/genomic-obj transposon)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
