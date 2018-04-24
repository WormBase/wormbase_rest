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

(defn print-sequence [c]
  {:data (when-let [p (:locatable-parent c)]
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
   :predicted_exon_structure predicted-exon-structure
   :print_homologies print-homologies
   :print_blast print-blast
   :predicted_unit generic/predicted-units
   :print_sequence print-sequence})
