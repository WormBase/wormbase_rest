(ns rest-api.classes.protein.widgets.sequences
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as str]
    [rest-api.db.sequence :as wb-seq]
    [rest-api.classes.generic-fields :as generic])
  (:import (org.biojava.nbio.core.sequence ProteinSequence DNASequence)
           (org.biojava.nbio.aaproperties PeptideProperties)))

(def abbrev
  {"A" "Ala"
   "R" "Arg"
   "N" "Asn"
   "D" "Asp"
   "C" "Cys"
   "E" "Glu"
   "Q" "Gln"
   "G" "Gly"
   "H" "His"
   "I" "Ile"
   "L" "Leu"
   "K" "Lys"
   "M" "Met"
   "F" "Phe"
   "O" "Pyl*"
   "P" "Pro"
   "S" "Ser"
   "T" "Thr"
   "W" "Trp"
   "Y" "Tyr"
   "V" "Val"
   "U" "Sec*"
   "X" "Xaa**"})

(defn estimated-isoelectric-point [p]
  {:data (when-let [h (:protein/peptide p)]
           (. PeptideProperties getIsoelectricPoint (:peptide/sequence (:protein.peptide/peptide h))))
   :description "the estimated isoelectric point of the protein"})

(defn estimated-molecular-weight [p]
  {:data (when-let [mw (:protein.molecular-weight/float
                         (first
                           (:protein/molecular-weight p)))]
           (format "%.1f" mw))
   :description "the estimated molecular weight of the protein"})

(defn amino-acid-composition [p]
  {:data 
   (let [source (when-let [h (:protein/peptide p)]
                  (->(:peptide/sequence (:protein.peptide/peptide h))
                                        (str/split  #"")
                                        (frequencies)))]
     (some->>
       (keys source)
       (map
         (fn [amino-key]
           {:comp (get source amino-key)
            :aa (get abbrev amino-key)}))))
   :description "The amino acid composition of the protein"})

(defn sequence-fn [p]
  {:data (when-let [h (:protein/peptide p)]
           {:length (:protein.peptide/length h)
            :sequences (:peptide/sequence
                         (:protein.peptide/peptide h))
            :type "aa"})
   :description "the peptide sequence of the protein"})

(def widget
  {:name generic/name-field
   :estimated_isoelectric_point estimated-isoelectric-point
   :estimated_molecular_weight estimated-molecular-weight
   :amino_acid_composition amino-acid-composition
   :sequence sequence-fn})
