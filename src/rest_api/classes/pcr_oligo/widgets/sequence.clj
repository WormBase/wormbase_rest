(ns rest-api.classes.pcr-oligo.widgets.sequence
  (:require
   [rest-api.classes.sequence.core :as sequence-fns]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn segment [p]
  {:data (if (contains? p :oligo/id);e.g. OK2488_external_b
           {:dna (:oligo/sequence p)
            :length (:oligo/length p)}
           (when-let [refseqobj (sequence-fns/genomic-obj p)] ;e.g. cenix:10-b12
             (let [s (sequence-fns/get-sequence refseqobj)]
               {:dna s
                :length (count s)
                :start (:start refseqobj)
                :stop (:stop refseqobj)
                :end (:stop refseqobj)
                :ref (:seqname refseqobj)
                :refseq (:seqname refseqobj)})))
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
