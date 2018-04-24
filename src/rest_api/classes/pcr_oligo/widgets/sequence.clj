(ns rest-api.classes.pcr-oligo.widgets.sequence
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn segment [p]
  {:data (keys p) ; cant find sequence
   :d (:db/id p)
   :description "Sequence/segment data about this PCR product"})

(defn oligos [p]
  {:data (some->> (:pcr-product/oligo p)
                  (map :pcr-product.oligo/oligo)
                  (map (fn [o]
                         {:obj (pack-obj o)
                          :sequence (:oligo/sequence o)})))
   :description "Oligos of this PCR product"})

(def widget
  {:name generic/name-field
   :segment segment
   :oligos oligos})
