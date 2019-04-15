(ns rest-api.classes.transcript.widgets.sequences
  (:require
    [datomic.api :as d]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]))

(defn strand [t]
  {:data (when-let [strand (:locatable/strand t)]
           (cond
             (= strand :locatable.strand/negative) "-"
             (= strand :locatable.strand/positive) "+"))
   :description "strand orientation of the sequence"})

(defn unspliced-sequence-context-with-padding [t]
  {:data (sequence-fns/transcript-sequence-features t 2001 "unspliced")
   :description "the unpliced sequence of the sequence"})

(defn unspliced-sequence-context [t]
  {:data (sequence-fns/transcript-sequence-features t 0 "unspliced")
   :description "the unpliced sequence of the sequence"})

(defn spliced-sequence-context [t]
  {:data (sequence-fns/transcript-sequence-features t 0 "spliced-with-utr")
   :description "the spliced sequence of the sequence"})

(defn protein-sequence [t]
  {:data (when-let [peptide (some->> (:transcript/corresponding-protein t)
                                     (:transcript.corresponding-protein/protein)
                                     (:protein/peptide)
                                     (:protein.peptide/peptide)
                                     (:peptide/sequence))]
           {:sequence peptide})
   :desciprion "The sequence of the protein"})

(def widget
  {:name generic/name-field
   :predicted_exon_structure generic/predicted-exon-structure
   :strand strand
   :protein_sequence protein-sequence
   :predicted_units generic/predicted-units
   :unspliced_sequence_context_with_padding unspliced-sequence-context-with-padding
   :unspliced_sequence_context unspliced-sequence-context
   :spliced_sequence_context spliced-sequence-context})
