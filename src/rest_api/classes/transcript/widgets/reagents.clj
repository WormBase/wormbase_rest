(ns rest-api.classes.transcript.widgets.reagents
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn source-clone [t]
  {:data (first
           (some->> (:sequence/clone
                      (:locatable/parent t))
                    (map pack-obj)))
   :description "The Source clone of the sequence"})

(defn matching-cdnas [t]
  {:data (some->> (map
                    :transcript.matching-cdna/sequence
                    (:transcript/matching-cdna t))
                  (map pack-obj))
   :description "cDNAs that match the transcript"})

(defn microarray-assays [t]
  {:data nil ; can't find example
   :description "The Microarray assays in this region of the sequence"})

(defn orfeome-assays [t]
  {:data nil ; based on the perl code should never exist
   :description "The ORFeome Assays of the sequence"})

(defn pcr-product [t]
  {:data (some->> (:transcript/corresponding-pcr-product t)
                  (map pack-obj))
   :description "PCR products for the sequence"})

(def widget
  {:name generic/name-field
   :microarray_assays microarray-assays
   :orfeome_assays orfeome-assays
   :source_clone source-clone
   :pcr_product pcr-product
   :matching_cdnas matching-cdnas})
