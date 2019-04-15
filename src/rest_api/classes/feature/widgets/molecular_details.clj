(ns rest-api.classes.feature.widgets.molecular-details
  (:require
    [clojure.string :as str]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.classes.generic-fields :as generic]))

; This was being displayed instead of the sequence if the sequence was not in the database
;{:flanks flanks  ; section tested with WBsf000519
; :seq seq-obj
; :method method
; :sequences [{:sequence five-prime-flank
;              :comment "flanking sequence (upstream)"}
;             {:sequence three-prime-flank
;              :comment "flanking sequence (downstream)"}]}

(defn sequence-context [f]
  {:data (when-let [s (:locatable/parent f)]
           (let [five-prime-flank (:feature.flanking-sequences/five-prime (:feature/flanking-sequences f))
                 three-prime-flank (:feature.flanking-sequences/three-prime (:feature/flanking-sequences f))
                 flanks [five-prime-flank three-prime-flank]
                 seq-obj (pack-obj s)
                 method (-> f :locatable/method :method/id)]
             (when (not (or (some #(= method %) ["SL1" "SL2" "polyA_site" "history feature"])
                     (not (contains? f :locatable/min))))
               (let [refseqobj (sequence-fns/genomic-obj f) ;tested with WBsf019129
                     positive-strand-wide (sequence-fns/get-sequence
                                 (conj
                                   refseqobj
                                   {:start (- (:start refseqobj) (max (count five-prime-flank) (count three-prime-flank)))
                                    :stop (+ (max (count five-prime-flank) (count three-prime-flank)) (:stop refseqobj))}))
                     strand (if (and (str/includes? positive-strand-wide five-prime-flank)
                                     (str/includes? positive-strand-wide three-prime-flank))
                              "+" "-")
                     padding 30
                     positive-sequence-raw (sequence-fns/get-sequence
                                         (conj
                                           refseqobj
                                           {:start (- (:start refseqobj) padding)
                                            :stop (+ (:stop refseqobj) padding)}))
                     feature-seq (str/upper-case (sequence-fns/get-sequence refseqobj))
                     feature-length (+ 1 (- (:stop refseqobj) (:start refseqobj)))
                     positive-sequence (str
                                         (subs positive-sequence-raw 0 padding)
                                         (str/upper-case (subs positive-sequence-raw padding (+ padding feature-length)))
                                         (subs positive-sequence-raw (+ padding feature-length) (count positive-sequence-raw)))
                     negative-sequence (generic-functions/dna-reverse-complement positive-sequence)]
                 {:flanks flanks
                  :feature_seq feature-seq
                  :seq seq-obj
                  :reported_on_strand strand
                  :dna-seq feature-seq
                  :sequences {:positive_strand
                              {:features
                               [{:type "feature"
                                 :start (+ 1 padding)
                                 :stop  (+ padding (count feature-seq))}]
                               :sequence positive-sequence}
                              :negative_strand
                              {:features
                               [{:type "feature"
                                 :start (+ 1 padding)
                                 :stop (+ padding feature-length)}]
                                :sequence negative-sequence}}}))))
   :description "sequences flanking the feature"})

(defn dna-text [f] ; tested with WBsf019129
  {:data (:feature/dna-text f)
   :description "DNA text of the sequence feature"})

(defn so-term [f]
  {:data (some->> (:feature/so-term f)
                  (map pack-obj))
   :description "SO term for feature"})

(def widget
  {:name generic/name-field
   :so_term so-term
   :sequence_context sequence-context
   :dna_text dna-text})
