(ns rest-api.classes.transcript.widgets.reagents
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn source-clone [t]
  {:data (some->> (:sequence/clone
                    (:locatable/parent t))
                  (map pack-obj)
                  (first))
   :description "The Source clone of the sequence"})

(defn matching-cdnas [t]
  {:data (some->> (map
                    :transcript.matching-cdna/sequence
                    (:transcript/matching-cdna t))
                  (map pack-obj)
                  (sort-by (fn [s] (str/lower-case (:label s)))))
   :description "cDNAs that match the transcript"})

(defn pcr-product [t]
  {:data (some->> (:transcript/corresponding-pcr-product t)
                  (map pack-obj)
                  (sort-by :label))
   :description "PCR products for the sequence"})

(def widget
  {:name generic/name-field
   :microarray_assays generic/microarray-assays
   :orfeome_assays generic/orfeome-assays
   :source_clone source-clone
   :pcr_product pcr-product
   :matching_cdnas matching-cdnas})
