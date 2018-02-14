(ns rest-api.classes.gene.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [gene]
  {:data (if (:gene/corresponding-transposon gene)
           ["TRANSPOSONS"
            "TRANSPOSON_GENES"]
           ["GENES"
            "VARIATIONS_CLASSICAL_ALLELES"
            "CLONES"])
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [gene]
  {:data (if (:gene/corresponding-transposon gene)
          "Transposons,Transposon Genes"
          "Curated_Genes,Classical_alleles,YACs_Fosmids_Cosmids")
   :description "tracks displayed in JBrowse"})

(defn genomic-image [gene]
  {:data (sequence-fns/genomic-obj gene)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
