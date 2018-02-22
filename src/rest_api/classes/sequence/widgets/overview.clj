(ns rest-api.classes.sequence.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn source-clone [s]
  {:data (when-let [clones (:sequence/clone s)]
           (pack-obj (first clones)))
   :description "The Source clone of the sequence"})

(defn cdss [s]
  {:data (when-let [chs (:cds.matching-cdna/_sequence s)]
           (into
             {}
             (for [ch chs
                   :let [cds (:cds/_matching-cdna ch)
                         id (:cds/id cds)]]
               {id (pack-obj cds)})))
   :description "Matching CDSs"})

(defn pseudogenes [s] ;OSTR077F5_1
  {:data (when-let [ph (first (:pseudogene.matching-cdna/_sequence s))]
             (pack-obj (:pseudogene/_matching-cdna ph)))
   :description "Matching Pseudogenes"})

(defn transcripts [s]
  {:data (when-let [ths (:transcript.matching-cdna/_sequence s)]
           (for [th ths
                 :let [t (:transcript/_matching-cdna th)]]
             {(:transcript/id t) (pack-obj t)}))
   :description "Matching Transcripts"})

(defn paired-read [s] ;OSTR077F5_1
  {:data (when-let [p (:sequence/paired-read s)]
           (pack-obj (first p)))
   :description "paired read of the sequence"})

(defn description [s]
  {:data (:sequence/title s)
   :description (str "description of the Sequence " (:sequence/id s))})

(defn analysis [s]
  {:data (when-let [a (:sequence/analysis s)]
           (pack-obj a))
   :dscription "The Analysis info of the sequence"})

(defn subsequence [s]
  {:data nil ; no subsequence field
   :key (keys s)
   :d (:db/id s)
   :description "end sequence reads used for initially placing the Fosmid on the genome "})

(def widget
  {:name generic/name-field
   :available_from generic/available-from
   :source_clone source-clone
   :cdss cdss
   :sequence_type generic/sequence-type
   :pseudogenes pseudogenes
   :remarks generic/remarks
   :method generic/method
   :transcripts transcripts
   :laboratory generic/laboratory
   :paired_read paired-read
   :taxonomy generic/taxonomy
   :description description
   :analysis analysis
   :identity generic/identity-field
   :subsequence subsequence})
