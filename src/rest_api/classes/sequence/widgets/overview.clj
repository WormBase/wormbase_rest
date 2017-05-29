(ns rest-api.classes.sequence.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn available-from [s]
  {:data nil
   :description "availability of clones of the sequence"})

(defn source-clone [s]
  {:data nil
   :description "The Source clone of the sequence"})

(defn cdss [s]
  {:data nil
   :description "Matching CDSs"})

(defn sequence-type [s]
  {:data nil
   :description "the general type of the sequence"})

(defn pseudogenes [s]
  {:data nil
   :description "Matching Pseudogenes"})

(defn method [s]
  {:data nil
   :description "the method used to describe the Sequence"})

(defn transcripts [s]
  {:data nil
   :description "Matching Transcripts"})

(defn laboratory [s]
  {:data nil
   :description "the laboratory where the Sequence was isolated, created, or named"})

(defn paired-read [s]
  {:data nil
   :description "paired read of the sequence"})

(defn analysis [s]
  {:data nil
   :dscription "The Analysis info of the sequence"})

(defn identity-field [s]
  {:data nil
   :description "Brief description of the genomic"})

(defn subsequence [s]
  {:data nil
   :description "end sequence reads used for initially placing the Fosmid on the genome "})

(def widget
  {:name generic/name-field
   :available_from available-from
   :source_clone source-clone
   :cdss cdss
   :sequence_type sequence-type
   :pseudogenes pseudogenes
   :remarks generic/remarks
   :method method
   :transcripts transcripts
   :laboratory laboratory
   :paired_read paired-read
   :taxonomy generic/taxonomy
   :description generic/description
   :analysis analysis
   :identity identity-field
   :subsequence subsequence})
