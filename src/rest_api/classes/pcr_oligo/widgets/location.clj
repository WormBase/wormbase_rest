(ns rest-api.classes.pcr-oligo.widgets.location
  (:require
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [pcr]
  {:data ["GENES"
          "MICROARRAY_OLIGO_PROBES"
          "PCR_PRODUCTS"
          "ORFEOME_PCR_PRODUCTS"
          "CLONES"]
   :description "tracks displayed in GBrowse"})

(defn jbrowse-tracks [pcr]
  {:data "Curated_Genes%2CMicroarray%20oligo%20probes%2CPCR%20Assays%2CORFeome%20PCR%20Products%2CYACs_Fosmids_Cosmids"
   :description "tracks displayed in JBrowse"})

(defn genomic-image [pcr]
  {:data (sequence-fns/genomic-obj pcr)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :jbrowse_tracks jbrowse-tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
