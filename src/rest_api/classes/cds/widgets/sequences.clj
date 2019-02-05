(ns rest-api.classes.cds.widgets.sequences
  (:require
    [clojure.string :as str]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn unspliced-sequence-context [c]
  {:data (when-let [transcript  (-> c
                                    :transcript.corresponding-cds/_cds
                                    first
                                    :transcript/_corresponding-cds)]
          (sequence-fns/transcript-sequence-features transcript 0 "unspliced"))
   :description "the unpliced sequence of the sequence"})

(defn spliced-sequence-context [c]
  {:data (when-let [transcript  (-> c
                                    :transcript.corresponding-cds/_cds
                                    first
                                    :transcript/_corresponding-cds)]
          (sequence-fns/transcript-sequence-features transcript 0 "spliced"))
   :description "the unpliced sequence of the sequence"})

(defn protein-sequence [c]
  {:data (when-let [peptide (some->> (:cds/corresponding-protein c)
                                     (:cds.corresponding-protein/protein)
                                     (:protein/peptide)
                                     (:protein.peptide/peptide)
                                     (:peptide/sequence))]
           {:sequence peptide})
   :desciprion "The sequence of the protein"})

(defn print-blast [c]
  {:data {:source (:cds/id c)
          :target (keys
                    (pace-utils/vmap
                      "Elegans genome"
                      true

                      "Elegans protein"
                      (when (contains? c :cds/corresponding-protein)
                        true)))}
   :description "links to BLAST analyses"})

(def widget
  {:name generic/name-field
   :predicted_exon_structure generic/predicted-exon-structure
   :print_blast print-blast
   :protein_sequence protein-sequence
   :predicted_unit generic/predicted-units
   :unspliced_sequence_context unspliced-sequence-context
   :spliced_sequence_context spliced-sequence-context})
