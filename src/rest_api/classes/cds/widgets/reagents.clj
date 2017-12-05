(ns rest-api.classes.cds.widgets.reagents
  (:require
    [rest-api.classes.generic-fields :as generic]))

(defn microarray-assays [c]
  {:data nil
   :description "The Microarray assays in this region of the sequence"})

(defn orfeome-assays [c]
  {:data nil
   :description "The ORFeome Assays of the sequence"})

(defn source-clone [c]
  {:data (first
           (some->> (:sequence/clone
                      (:locatable/parent c))
                    (map pack-obj)))
   :description "The Source clone of the sequence"})

(defn pcr-products [c]
  {:data (some->> (:cds/corresponding-pcr-product c)
                  (map pack-obj))
   :description "PCR products for the sequence"})

(defn matching-cdnas [c]
  {:data nil
   :description "cDNAs that match the sequence"})

(def widget
  {:name generic/name-field
   :microarray_assays microarray-assays
   :orfeome_assays orfeome-assays
   :source_clone source-clone
   :pcr_products pcr-products
   :matching_cdnas matching-cdnas})
