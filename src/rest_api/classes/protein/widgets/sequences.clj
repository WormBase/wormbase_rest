(ns rest-api.classes.protein.widgets.sequences
  (:require
    [rest-api.classes.generic-fields :as generic]))

(defn estimated-isoelectric-point [p]
  {:data nil ; should use org.biojava.bio.proteomics.IsoelectricPointCalc see: http://biojava.org/wiki/BioJava%3ACookbook%3AProteomics
   :description "the estimated isoelectric point of the protein"})

(defn estimated-molecular-weight [p]
  {:data (when-let [mw (:protein.molecular-weight/float
           (first
             (:protein/molecular-weight p)))]
           (format "%.1f" mw))
   :description "the estimated molecular weight of the protein"})

(defn amino-acid-composition [p]
  {:data (keys p)
   :d (:db/id p) ; bioperl uses seqStats
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
