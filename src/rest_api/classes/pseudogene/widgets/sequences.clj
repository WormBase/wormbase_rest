(ns rest-api.classes.pseudogene.widgets.sequences
  (:require
   [clojure.string :as str]
   [rest-api.classes.sequence.core :as sequence-fns]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))


(defn strand [p]
  {:data (when-let [strand-kw (:locatable/strand p)]
           (name strand-kw))
   :description "strand orientation of the sequence"})

(defn print-sequence [p]
  {:data (when-let [refseqobj (sequence-fns/genomic-obj p)]
           (if-let [dna-sequence (sequence-fns/get-sequence refseqobj)]
             (let [strand (if-let [strand-kw (:locatable/strand p)]
                            (name strand-kw)
                            "positive")
                   dna-sequence (if (= strand "negative")
                                  (generic-functions/dna-reverse-complement dna-sequence)
                                  dna-sequence)]

               {:length (count dna-sequence)
                :positive_strand
                {:sequence dna-sequence}
                :negative_strand
                {:sequence (generic-functions/dna-reverse-complement p)}
                :strand (if (= strand "positive") "+" "-")
                :header "Sequence"})))
   :descriptions "The sequence of the pseudogene"})

(def widget
  {:name generic/name-field
   :predicted_exon_structure generic/predicted-exon-structure
   :strand strand
   :print_sequence print-sequence})
