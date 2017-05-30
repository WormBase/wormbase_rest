(ns rest-api.classes.sequence.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
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

(defn sequence-type [s] ; this comes from the properties section in the tree view. But doesn't seem to fully line up
  {:data (cond
           (contains? s :sequence/cdna)
           (let [cdna (name (first (:sequence/cdna s)))]
             (case cdna
               "cdna-est" "cDNA_EST"
               "est-5" "EST_5"
               "est-3" "EST_3"
               "capped-5" "Capped_5"
               "tsl-tag" "TSL_tag"
               cdna))
           
           (contains? s :sequence/genomic)
           "Genomic")
   :description "the general type of the sequence"})

(defn pseudogenes [s] ;OSTR077F5_1
  {:data (when-let [ph (first (:pseudogene.matching-cdna/_sequence s))]
             (pack-obj (:pseudogene/_matching-cdna ph)))
   :description "Matching Pseudogenes"})

(defn remarks [s]
  {:data (if-let [rhs (:sequence/db-remark s)]
           (for [rh rhs]
             {:text (:sequence.db-remark/text rh)
              :evidence (obj/get-evidence rh)})
           (when-let [rhs (:sequence/remark s)]
             (for [rh rhs]
               {:text (:sequence.remark/text rh)
                :evidence (obj/get-evidence rh)})))
   :description "curatorial remarks for the Sequence"})

(defn method [s]
  {:data (when-let [method (:locatable/method s)]
           {:method (:method/id method)
            :details (:method.remark/text (first (:method/remark method)))})
   :description "the method used to describe the Sequence"})

(defn transcripts [s]
  {:data (when-let [ths (:transcript.matching-cdna/_sequence s)]
           (for [th ths
                 :let [t (:transcript/_matching-cdna th)]]
             {(:transcript/id t) (pack-obj t)}))
   :description "Matching Transcripts"})

(defn laboratory [s]
  {:data (when-let [lab (:sequence/from-laboratory s)]
           {:laboratory  (pack-obj lab)
            :representative (when-let [reps (:laboratory/representative lab)]
                               (for [rep reps] (pack-obj rep)))})
   :description "the laboratory where the Sequence was isolated, created, or named"})

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

(defn identity-field [s]
  {:data nil ; no brief-identification field
   :description "Brief description of the genomic"})

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
   :sequence_type sequence-type
   :pseudogenes pseudogenes
   :remarks remarks
   :method method
   :transcripts transcripts
   :laboratory laboratory
   :paired_read paired-read
   :taxonomy generic/taxonomy
   :description description
   :analysis analysis
   :identity identity-field
   :subsequence subsequence})
