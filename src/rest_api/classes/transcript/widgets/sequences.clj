(ns rest-api.classes.transcript.widgets.sequences
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic-fields :as generic]))

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
  ;:predicted_exon_structure generic/predicted-exon-structure
  ;:strand strand
   :predicted_units generic/predicted-units
   :transcripts transcripts
   :print_sequence generic/print-sequence})
