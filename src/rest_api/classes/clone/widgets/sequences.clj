(ns rest-api.classes.clone.widgets.sequences
  (:require
    [clojure.string :as str]
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

(defn predicted-units [c]
  {:data (some->> (:locatable/_parent
                    (first
                      (:sequence/_clone c)))
                  (map (fn [child]
                         (some->> (or
                                    (:gene.corresponding-cds/_cds child)
                                    (:gene.corresponding-transcript/_transcript child))
                                  (map (fn [h]
                                         (or
                                           (:gene/_corresponding-cds h)
                                           (:gene/_corresponding-transcript h))))
                                  (map (fn [g]
                                         (let [low (:locatable/min child)
                                               high (:locatable/max child)]
                                           {:comment (or
                                                       (:cds.brief-identification/text
                                                         (:cds/brief-identification child))
                                                       (or
                                                         (:transcript/brief-identification child)
                                                         (or
                                                           (some->> (:cds/db-remark child)
                                                                    (map :cds.db-remark/text)
                                                                    (str/join "<br />"))
                                                           (or
                                                             (some->> (:transcript/db-remark child)
                                                                      (map :transcript.db-remark/text)
                                                                      (str/join "<br />"))
                                                             (or
                                                               (some->> (:cds/remark child)
                                                                        (map :cds.remark/text)
                                                                        (str/join "<br />"))
                                                               (or
                                                                 (some->> (:transcript/remark child)
                                                                          (map :transcript.remark/text)
                                                                          (str/join "<br />"))
                                                                 "&nbsp;"))))))
                                            :predicted_type (if (contains? child :transcript/id)
                                                              "Transcript"
                                                              "CDS")
                                            :gene (pack-obj g)
                                            :name (pack-obj child)
                                            :start (if (> low high) low high)
                                            :end (+ (if (< low high) low high) 1)}))))))
                  (flatten)
                  (remove nil?)
                  (not-empty))
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
