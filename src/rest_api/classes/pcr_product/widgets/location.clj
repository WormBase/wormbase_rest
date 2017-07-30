(ns rest-api.classes.pcr-product.widgets.location
  (:require
    [rest-api.classes.sequence.main :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn tracks [pcr-product]
  {:data ["GENES"
          "MICROARRAY_OLIGO_PROBES"
          "PCR_PRODUCTS"
          "ORFEOME_PCR_PRODUCTS"
          "CLONES"]
   :description "tracks displayed in GBrowse"})

(defn genomic-image [pcr-product]
  {:data (sequence-fns/genomic-obj pcr-product)
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
    {:name generic/name-field
     :genetic_position generic/genetic-position
     :tracks tracks
     :genomic_position generic/genomic-position
     :genomic_image genomic-image})
