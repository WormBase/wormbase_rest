(ns rest-api.classes.sequence.widgets.reagents
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn source-clone [s]
  {:data (some->> (:sequence/clone
                    (:locatable/parent s))
                  (map pack-obj)
                  (first))
   :description "The Source clone of the sequence"})

(defn matching-cdnas [s]
  {:data (some->> (map :sequence/matching-cdna s)
                  (map pack-obj))
   :description "cDNAs that match the sequence"})

(defn pcr-products [s]
  {:data (some->> (:locatable/_parent s)
                  (map (fn [f]
                         (some->> (:transcript/corresponding-pcr-product f)
                                  (map (fn [p]
                                         {(:pcr-product/id p) (pack-obj p)})))))
                  (flatten)
                  (into {})
                  (vals)
                  (sort-by :label))
   :description "PCR products for the sequence"})

(def widget
  {:name generic/name-field
   :laboratory generic/laboratory
   :microarray_assays generic/microarray-assays
   :orfeome_assays generic/orfeome-assays
   :source_clone source-clone
   :pcr_products pcr-products
   :matching_cdnas matching-cdnas})
