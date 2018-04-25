(ns rest-api.classes.sequence.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn source-clone [s]
  {:data (some->> (:sequence/clone s)
                  (first)
                  (pack-obj))
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
  {:data (some->> (:transcript.matching-cdna/_sequence s)
                  (map :transcript/_matching-cdna)
                  (map (fn [t]
                         {(:transcript/id t) (pack-obj t)}))
                  (into {}))
   :description "Matching Transcripts"})

(defn paired-read [s] ;OSTR077F5_1
  {:data (some->> (:sequence/paired-read s)
                  (first)
                  (pack-obj))
   :description "paired read of the sequence"})

(defn description [s]
  {:data (:sequence/title s)
   :description (str "description of the Sequence " (:sequence/id s))})

(defn analysis [s]
  {:data (when-let [a (:sequence/analysis s)]
           (pack-obj a))
   :dscription "The Analysis info of the sequence"})

(defn subsequence [s]
  {:data (not-empty
           (some->> (:sequence/clone s)
                    (map (fn [c]
                           (some->> (:sequence/_clone-end-seq-read c)
                                    (map pack-obj))))
                    (flatten)
                    (remove nil?)
                    (sort-by :label)))
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
