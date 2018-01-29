(ns rest-api.classes.feature.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [feature]
  {:data ["GENES"
          "RNASEQ_ASYMMETRIES"
          "RNASEQ"
          "RNASEQ_SPLICE"
          "POLYSOMES"
          "MICRO_ORF"
          "DNASEI_HYPERSENSITIVE_SITE"
          "REGULATORY_REGIONS"
          "PROMOTER_REGIONS"
          "HISTONE_BINDING_SITES"
          "TRANSCRIPTION_FACTOR_BINDING_REGION"
          "TRANSCRIPTION_FACTOR_BINDING_SITE"
          "GENOME_SEQUENCE_ERROR_CORRECTED"
          "BINDING_SITES_PREDICTED"
          "BINDING_SITES_CURATED"
          "BINDING_REGIONS"
          "GENOME_SEQUENCE_ERROR"]
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [feature]
  {:data "Curated_Genes%2CRNASeq%20Asymmetries%2CRNASeq%2CRNASeq%20introns%2CPolysomes%2CDNAseI%20hypersensitive%20site%2CRegulatory%20regions%2CPromoter%20regions%2CHistone%20binding%20sites%2CTranscription%20factor%20binding%20regions%2CTranscription%20factor%20binding%20sites%2CGenome%20sequence%20corrections%2CBinding%20sites%20(predicted)%2CBinding%20sites%20(curated)%2CBinding%20regions%2CGenome%20sequence%20errors"
   :description "tracks displayed in JBrowse"})

(defn genomic-image [feature]
  {:data (sequence-fns/genomic-obj feature)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
