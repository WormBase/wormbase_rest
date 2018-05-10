(ns rest-api.classes.cds.widgets.reagents
  (:require
    [clojure.string :as str]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn source-clone [c]
  {:data (some->> (:sequence/clone
                    (:locatable/parent c))
                  (map pack-obj)
                  (first))
   :description "The Source clone of the sequence"})

(defn pcr-products [c]
  {:data (some->> (:cds/corresponding-pcr-product c)
                  (map pack-obj)
                  (sort-by (fn [s] (str/lower-case (:label s)))))
   :description "PCR products for the sequence"})

(defn matching-cdnas [c]
  {:data (some->> (:cds/matching-cdna c)
                  (map :cds.matching-cdna/sequence)
                  (map pack-obj)
                  (sort-by :label))
   :description "cDNAs that match the sequence"})

(def widget
  {:name generic/name-field
   :microarray_assays generic/microarray-assays
   :orfeome_assays generic/orfeome-assays
   :source_clone source-clone
   :pcr_products pcr-products
   :matching_cdnas matching-cdnas})
