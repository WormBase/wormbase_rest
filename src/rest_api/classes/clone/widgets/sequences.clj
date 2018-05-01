(ns rest-api.classes.clone.widgets.sequences
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn strand [c]
  {:data (when-let [seg (first (:data (generic/genomic-position c)))]
           (if (< (:start seg) (:stop seg)) "+" "-"))
   :description "strand orientation of the sequence"})

(defn predicted-units [c]
  {:data (some->> (:locatable/_parent
                    (first
                      (:sequence/_clone c)))
                  (map (fn [child]
                         (some->> (:gene.corresponding-transcript/_transcript child)
                                  (map (fn [h]
                                         (:gene/_corresponding-transcript h)))
                                  (map (fn [g]
                                         (let [lmin (:locatable/min child)
                                               lmax (:locatable/max child)
                                               strand (when-let [strand-kw (:locatable/strand child)]
                                                        (name strand-kw))]
                                           {:comment (or
                                                       (:transcript/brief-identification child)
                                                       (some->> (:transcript/remark child)
                                                                (map :transcript.remark/text)
                                                                (str/join "<br />")))
                                            :bio_type (-> child
                                                          :locatable/method
                                                          :method/gff-source
                                                          (str/replace #"non_coding" "non-coding")
                                                          (str/replace #"_" " "))
                                            :predicted_type (-> child
                                                                :locatable/method
                                                                :method/gff-source
                                                                (str/replace #"non_coding" "non-coding")
                                                                (str/replace #"_" " "))
                                            :gene (pack-obj g)
                                            :name (pack-obj child)
                                            :s strand
                                            :start (if (= strand "negative") lmax lmin)
                                            :end (if (= strand "negative") lmin lmax)}))))))
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
   :print_sequence print-sequence
   :strand strand})
