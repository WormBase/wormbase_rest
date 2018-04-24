(ns rest-api.classes.sequence.widgets.sequences
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.sequence.core :as sequence-core]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn strand [s]
  {:data (case (some->> (:locatable/_parent s)
                        (map :locatable/strand)
                        (remove nil?)
                        (first)
                        (name))
           "negative"
           "-"

           "positive"
           "+"

           "unknown")
   :description "strand orientation of the sequence"})

(defn print-sequence [s]
  {:data (let [h (:sequence/dna s)]
           {:header "Sequence"
            :sequence (-> h
                          :sequence.dna/dna
                          :dna/sequence)
            :length (:sequence.dna/length h)})
   :description "the sequence of the sequence"})

(def widget
  {:name generic/name-field
   :strand strand
   :predicted_units generic/predicted-units
   :print_sequence print-sequence})
