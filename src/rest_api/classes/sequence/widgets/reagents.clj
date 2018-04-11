(ns rest-api.classes.sequence.widgets.reagents
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn source-clone [s]
  {:data (some->> (:sequence/clone
                    (:locatable/parent s))
                  (map pack-obj)
                  (first))
   :description "The Source clone of the sequence"})

(defn matching-cdnas [s]
  {:data nil;(some->> (map :sequence/matching-cdna s)
                 ; (map pack-obj))
   :description "cDNAs that match the sequence"})

(defn microarray-assays [s]
  {:data (keys (:locatable/assembly-parent s))
   :k (keys s)
   :d (:db/id s)
   :description "The Microarray assays in this region of the sequence"})

(defn orfeome-assays [s]
  {:data nil ; based on the perl code should never exist
   :description "The ORFeome Assays of the sequence"})

(defn pcr-product [s]
  {:data (some->> (:sequence/corresponding-pcr-product s)
                  (map pack-obj))
   :description "PCR products for the sequence"})

(def widget
  {:name generic/name-field
   :laboratory generic/laboratory
   :microarray_assays microarray-assays
   :orfeome_assays orfeome-assays
   :source_clone source-clone
   :pcr_product pcr-product
   :matching_cdnas matching-cdnas})
