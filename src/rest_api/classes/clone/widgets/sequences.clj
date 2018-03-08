(ns rest-api.classes.clone.widgets.sequences
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn strand [c] ; this does work for WRM0612cD02 but not JC8 or FN891036
  {:data (when-let [strand (some->> (:sequence/_clone c)
                                    (map :locatable/strand)
                                    (remove nil?)
                                    (first))]
           (case (name strand)
             "negative" "-"
             "positive" "+"))
   :description "strand orientation of the sequence"})

;; need to use cds if available
(defn predicted-units [c]
  {:data (some->> (:clone/positive-gene c)
                  (map :clone.positive-gene/gene)
                  (map (fn [g]
                         (some->> (:gene/corresponding-transcript g)
                                  (map :gene.corresponding-transcript/transcript)
                                  (map (fn [t]
                                         (let [low (:locatable/min t)
                                               high (:locatable/max t)]
                                           {:comment nil
                                            :predicted_type (:transcript/id t)
                                            :gene (pack-obj g)
                                            :name (pack-obj t)
                                            :start (if (> low high) low high)
                                            :end (+ (if (< low high) low high) 1)
                                            :k (keys t)
                                            }
                                           )))
                                  )))
                  (flatten)

                  )
   :d (:db/id c)
   :description "features contained within the sequence"})

(defn end-reads [c]
  {:data (some->> (:sequence/_clone-end-seq-read c)
                 (map pack-obj))
   :description "end reads associated with this clone"})

(defn sequences [c]
  {:data (some->> (:sequence/_clone c)
                  (map pack-obj))
   :description "sequences associated with this clone"})

(defn transcripts [c] ; e.g. FN891036
  {:data (some->> (:sequence/_clone c)
                  (map (fn [s]
                        (some->> (:transcript.matching-cdna/_sequence s)
                                 (map :transcript/_matching-cdna)
                                 (map pack-obj))))
                  (flatten)
                  (remove nil?)
                  (sort-by :label)
                  (not-empty))
   :description "Matching Transcripts"})

(defn print-sequence [c] ; works for JC8 does not work for WRM0612cD02
  {:data (some->> (:sequence/_clone c)
                  (map (fn [s]
                         (let [h (:sequence/dna s)]
                           {:header "Sequence"
                            :sequence (-> h
                                          :sequence.dna/dna
                                          :dna/sequence)
                            :length (:sequence.dna/length h)}))))
   :description "the sequence of the sequence"})

(def widget
  {:name generic/name-field
   :predicted_units predicted-units
   :end_reads end-reads
   :sequences sequences
   :transcripts transcripts
  ; :print_sequence print-sequence
   :strand strand})
