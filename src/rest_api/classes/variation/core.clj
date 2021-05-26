(ns rest-api.classes.variation.core
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(def ^{:private true} molecular-change-effects
  {:molecular-change/missense "Missense"
   :molecular-change/nonsense "Nonsense"
   :molecular-change/frameshift "Frameshift"
   :molecular-change/silent "Silent"
   :molecular-change/splice-site "Splice site"
   :molecular-change/promoter "Promoter"
   :molecular-change/genomic-neighbourhood "Genomic neighbourhood"
   :molecular-change/regulatory-feature "Regulatory feature"
   :molecular-change/readthrough "Readthrough"})

(defn- process-aa-change [molecular-change]
  (pace-utils/cond-let
   [n]
   (first (:molecular-change/missense molecular-change))
   (:molecular-change.missense/text n)

   (:molecular-change/nonsense molecular-change)
   (:molecular-change.nonsense/text n)))

(defn- process-aa-position [molecular-change]
  (pace-utils/cond-let
   [n]
   (first (:molecular-change/missense molecular-change))
   (:molecular-change.missense/int n)

   (:molecular-change/nonsense molecular-change)
   (nth (re-find #"\((\d+)\)"
                 (:molecular-change.nonsense/text n)) 1)))

(defn- change-set
  "Return a the set of keys of all maps in `change-map-seqs`."
  [& change-map-seqs]
  (->> (apply concat change-map-seqs)
       (mapcat keys)
       (set)))

;; TODO: split up and refactor this function.
(defn process-variation [var relevant-location?
                         & {:keys [window]
                            :or {window 20}}]
  (let [slice (comp seq (partial take window))
        cds-changes (->> (:variation/transcript var)
                         (:variation.transcript/transcript)
                         (:transcript/corresponding-cds)
                         (slice))
        trans-changes (->> (:variation/transcript var)
                           (slice))
        gene-changes (->> (:variation/gene var)
                          (filter #(relevant-location? (:variation.gene/gene %)))
                          (slice))]
   (pace-utils/vmap
     :variation
     (pack-obj "variation" var)

     :type
     (if (:variation/transposon-insertion var)
       "transposon insertion"
       (str/join ", "
                 (or
                  (pace-utils/those
                   (if (:variation/engineered-allele var)
                     "Engineered allele")
                   (if (:variation/allele var)
                     "Allele")
                   (if (:variation/snp var)
                     "SNP")
                   (if (:variation/confirmed-snp var)
                     "Confirmed SNP")
                   (if (:variation/predicted-snp var)
                     "Predicted SNP")
                   (if (:variation/reference-strain-digest var)
                     "RFLP"))
                  ["unknown"])))

     :method_name
     (if-let [method (:variation/method var)]
       (format "<a class=\"longtext\" tip=\"%s\">%s</a>"
               (or (:method.remark/text
                    (first (:method/remark methods)))
                   "")
               (str/replace (:method/id method) #"_" " ")))

     :gene
     (when-let [ghs (:variation/gene var)]
      (for [gh ghs
            :let [gene (:variation.gene/gene gh)]]
       (pack-obj gene)))

     :molecular_change
     (cond
       (:variation/substitution var)
       "Substitution"

       (:variation/insertion var)
       "Insertion"

       (:variation/deletion var)
       "Deletion"

       (:variation/inversion var)
       "Inversion"

       (:variation/tandem-duplication var)
       "Tandem_duplication"

       :default
       "Not curated")

     :locations
     (some->> trans-changes
              (map (fn [tc]
                    (pace-utils/those
                     (when (:molecular-change/intron tc)
                       "Intron")
                     (when (:molecular-change/coding-exon tc)
                       "Coding exon")
                     (when (:molecular-change/genomic-neighbourhood tc)
                       "Genomic Neighbourhood")
                     (when (:molecular-change/noncoding-exon tc)
                       "Noncoding Exon")
                     (when (:molecular-change/five-prime-utr tc)
                       "5' UTR")
                     (when (:molecular-change/three-prime-utr tc)
                       "3' UTR"))))
              (flatten)
              (distinct)
              (remove nil?)
              (str/join ", ")
              (not-empty))

     :effects
     (let [changes (change-set cds-changes gene-changes trans-changes)]
       (->> changes
            (map molecular-change-effects)
            (filter identity)
            (not-empty)))

     :composite_change
     (some->> trans-changes
              (map (fn [h]
                     (some->> (:molecular-change/amino-acid-change h)
                              (map :molecular-change.amino-acid-change/text)
                              (map (fn [s]
                                (str/join "<br />" 
                                 (map (partial apply str) (partition-all 20 s))))))))
              (flatten)
              (remove nil?)
              (distinct)
              (str/join ",<br /><br />")
              (not-empty))

     :aa_position
     (some->> trans-changes
              (map (fn [h]
                     (some->> (:molecular-change/protein-position h)
                              (first)
                              (:molecular-change.protein-position/text))))
              (remove nil?)
              (distinct)
              (str/join "<br />"))

     :sift
     (some->> trans-changes
              (map (fn [h]
                    (some->> (:molecular-change/sift h)
                             (first)
                             ((fn [sift]
                               (some-> (:molecular-change.sift/text sift)
                                       (str/replace "_" " ")))))))
              (flatten)
              (remove nil?)
              (distinct)
              (str/join "<br />"))

     :isoform
     (some->> trans-changes
              (map (fn [h]
                    (when (:molecular-change/cds-position h)
                     (some->> (:variation.transcript/transcript h)
                              (:transcript/corresponding-cds)
                              (:transcript.corresponding-cds/cds)
                              (pack-obj)))))
              (remove nil?)
              (first)
              (not-empty))

     :phen_count
     (count (:variation/phenotype var))

     :strain
     (->> (:variation/strain var)
          (map :variation.strain/strain)
          (map #(pack-obj "strain" %))
          (not-empty))

     :sources
     (let [var-refs (:variation/reference var)
           sources (if (empty? var-refs)
                     (map #(let [packed (pack-obj %)]
                             (into packed
                                   {:label
                                    (str/replace (:label packed)
                                                 #"_" " ")}))
                          (:variation/analysis var))
                     (map #(pack-obj (:variation.reference/paper %))
                          var-refs))]
           (not-empty sources)))))
