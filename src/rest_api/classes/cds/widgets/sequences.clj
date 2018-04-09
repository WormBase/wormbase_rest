(ns rest-api.classes.cds.widgets.sequences
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn predicted-exon-structure [c]
  {:data (some->> (:cds/source-exons c)
                 (map (fn [h]
                        (let [start (:cds.source-exons/start h)
                              end (:cds.source-exons/end h)
                              length (+ (- end start) 1)]
                          {:start start
                           :end end
                           :len length})))
                 (sort-by :start)
                 (map-indexed
                   (fn [idx o]
                     (conj {:no (+ 1 idx)} o))))
   :description "predicted exon structure within the sequence"})

(defn print-homologies [c]
  {:data nil
   :description "homologous sequences"})

(defn print-blast [c]
  {:data nil
   :description "links to BLAST analyses"})

(defn predicted-units [c]
  {:data (some->> (:locatable/_parent
                    (first
                      (:sequence/_cds c)))
                  (map (fn [child]
                         (some->> (:gene.corresponding-transcript/_transcript child)
                                  (map (fn [h]
                                         (:gene/_corresponding-transcript h)))
                                  (map (fn [g]
                                         (let [low (:locatable/min child)
                                               high (:locatable/max child)]
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
                                            :start (+ (if (> low high) high low) 1)
                                            :end (if (< low high) high low)}))))))
                  (flatten)
                  (remove nil?)
                  (not-empty))
   :description "features contained within the sequence"})

(defn print-sequence [c]
  {:data (when-let [p (:locatable-parent c)]
           (let [])
                  (map (fn [s]
                         (let [h (:sequence/dna s)]
                           {:header "Sequence"
                            :sequence (-> h
                                          :sequence.dna/dna
                                          :dna/sequence)
                            :length (:sequence.dna/length h)}))))
   :d (:db/id c)
   :description "the sequence of the sequence"})

(def widget
  {:name generic/name-field
   :predicted_exon_structure predicted-exon-structure
   :print_homologies print-homologies
   :print_blast print-blast
   :predicted_unit predicted-units
   :print_sequence print-sequence})
