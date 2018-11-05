(ns rest-api.classes.transcript.widgets.sequences
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic-fields :as generic]))

(defn predicted-exon-structure [s]
  {:data (some->> (:transcript/source-exons s)
                  (map (fn [exon]
                         {:start (:transcript.source-exons/min exon)
                          :stop (:transcript.source-exons/max exon)}))
                  (sort-by :start)
                  (map-indexed (fn [idx obj]
                         (conj obj
                               {:no (+ 1 idx)}))))
   :description "predicted exon structure within the sequence"})

(defn strand [s]
  {:data (when-let [strand (:locatable/strand s)]
           (cond
             (= strand :locatable.strand/negative) "-"
             (= strand :locatable.strand/positive) "+"))
   :description "strand orientation of the sequence"})

(defn transcripts [s]
  {:data nil
   :d (:db/id s)
   :description "Transcripts in this region of the sequence"})

(defn print-sequence [s]
  {:data nil
   :description "the sequence of the sequence"})

(def widget
  {:name generic/name-field
 ;  :predicted_exon_structure predicted-exon-structure
 ;  :strand strand
   :predicted_units generic/predicted-units
   :transcripts transcripts
   :print_sequence print-sequence})
