(ns rest-api.classes.cds.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [cds]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:cds/species cds)))
           (if (= "history" (:method/id (:locatable/method cds)))
             ["HISTORICAL_GENES"]
             ["GENES"
              "TRANSPOSONS"
              "TRANSPOSON_GENES"
              "EST_BEST"
              "PROTEIN_MOTIFS"]))
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [cds]
  {:data (when (= "Caenorhabditis elegans" (:species/id (:cds/species cds)))
           (if (= "history" (:method/id (:locatable/method cds)))
             "Gene%20Models%20(historical)"
             "Curated_Genes%2CTransposon%20Genes%2CTransposons%2CESTs%20(best)%2CProtein%20motifs"))
   :description "tracks displayed in JBrowse"})

(defn genomic-image [cds]
  {:data (sequence-fns/genomic-obj cds)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
