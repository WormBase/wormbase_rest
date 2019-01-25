(ns rest-api.classes.feature.widgets.molecular-details
  (:require
    [clojure.string :as str]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.classes.generic-fields :as generic]))


(defn flanking-sequences [f]
  {:data (when-let [s (:locatable/parent f)]
           (let [five-prime-flank (:feature.flanking-sequences/five-prime (:feature/flanking-sequences f))
                 three-prime-flank (:feature.flanking-sequences/three-prime (:feature/flanking-sequences f))
                 flanks [five-prime-flank three-prime-flank]
                 seq-obj (pack-obj s)
                 method (-> f :locatable/method :method/id)]
             (if (or (some #(= method %) ["SL1" "SL2" "polyA_site" "history feature"])
                     (not (contains? f :locatable/min)))
               {:flanks flanks  ; section tested with WBsf000519
                :seq seq-obj
                :sequences [{:sequence five-prime-flank
                             :comment "flanking sequence (upstream)"}
                            {:sequence three-prime-flank
                             :comment "flanking sequence (downstream)"}]})
             (let [feature-start (:locatable/min f) ;tested with WBsf019129
                   feature-end (:locatable/max f)
                   padding 30
                   refseqobj (sequence-fns/genomic-obj f)
                   s (sequence-fns/get-sequence
                       (conj
                         refseqobj
                         {:start (- (:start refseqobj) padding)
                          :stop (+ padding (:stop refseqobj))}))
                   padding-start (if (> (count five-prime-flank) padding)
                                   (subs five-prime-flank padding)
                                   five-prime-flank)
                   padding-end (if (> (count three-prime-flank) padding)
                                 (subs three-prime-flank
                                       (- (count three-prime-flank) padding)
                                       (count three-prime-flank))
                                 three-prime-flank)
                   strand (if (and (str/includes? s padding-start)
                                   (str/includes? s padding-end))
                            "positive" "negative")
                   processed-sequence (if (= strand "negative")
                                        (generic-functions/reverse-complement s)
                                        s)
                   feature-seq (str/upper-case
                                 (subs
                                   processed-sequence
                                   padding
                                   (- (count processed-sequence) padding)))]
               {:flanks flanks
                :feature_seq feature-seq
                :seq seq-obj
                :dna-seq feature-seq
                :sequences [{:sequence (str/replace
                                         processed-sequence
                                         (subs processed-sequence
                                               padding
                                               (- (count processed-sequence) padding))
                                         feature-seq)
                             :comment (str "upper case: feature sequence; lower case: flanking sequences (" strand ")")
                             :highlight
                             {:offset padding
                              :length (count feature-seq)}}]})))
   :description "sequences flanking the feature"})

(defn dna-text [f] ; tested with WBsf019129
  {:data (:feature/dna-text f)
   :description "DNA text of the sequence feature"})

(def widget
  {:name generic/name-field
   :flanking_sequences flanking-sequences
   :dna_text dna-text})
