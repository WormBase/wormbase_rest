(ns rest-api.classes.variation.widgets.molecular-details
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.db.sequence :as seqdb]
    [rest-api.classes.sequence.core :as sequence-fns]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-functions]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn polymorphism-type [variation]
  {:data nil
   :description "the general class of this polymorphism"})

(defn amino-acid-change [variation]
  {:data nil
   :description "amino acid changes for this variation, if appropriate"})

(defn detection-method [variation] ; WBVar00601206
  {:data (first (:variation/detection-method variation))
   :description "detection method for polymorphism, typically via sequencing or restriction digest."})

(defn deletion-verification [variation]
  {:data nil
   :description "the method used to verify deletion alleles"})

;has data
(defn context [variation]
  {:data (let [public-name (:variation/public-name variation)
               max-seqlen 1000000
               flank 250
               seq-len (if
                         (contains? variation :variation-source-location)
                         nil
                         1)
               ]

           {:ldtype_fragment nil
            :wildtype_full (seq (:variation/expr-pattern variation))
            :mutant_fragment nil
            :keys (keys variation)
            :mutant_full nil
            :wildtype_header (str "Wild type N2, with " flank " bp flanks")
            :sl (seq (:variation/source-location variation))
            :mutant_header (str name " with " flank " bp flanks")
            :placeholder nil})
   :description "wild type and variant sequences in genomic context"})

(defn flanking-pcr-products [variation]
  {:data nil
   :description "PCR products that flank the variation"})

;test WBVar01112111
(defn variation-type [variation]
  (let [variation-type-map {:variation/engineeered-allele "Engineered allele"
                            :variation/allele"Allele"
                            :variation/snp "SNP"
                            :variation/confirmed-snp "Confirmed SNP"
                            :variation/reference-strain-digest "RFLP"
                            :variation/predicted-snp "Predicted SNP"
                            :variation/transposon-insertion "Transposon Insertion"
                            :variation/natural-variant "Natural Variant"}
        type-of-mutation-map {:variation/substitution "Substitution"
                              :variation/insertion "Insertion"
                              :variation/deletion "Deletion"
                              :variation/tandem-duplication "Tandem Duplication"}]
    {:data {:physical_class (cond
                             (contains? variation :variation/transposon-insertion)
                             "Transposon insertion"

                             (contains? variation :variation/transposon-excision)
                             "Transposon excision"

                             :else
                             (->> type-of-mutation-map
                                  (map
                                    #(str (if
                                            (contains? variation (first %))
                                            (second %))))
                                  (filter #(not= % ""))
                                  (str/join "/")))
            :general_class (->> variation-type-map
                                (map
                                  #(str (if
                                          (contains? variation (first %))
                                          (second %))))
                                (filter #(not= % "")))}
     :description "the general type of the variation"}))

(defn- count-affects [variation]
  (reduce
    +
    (map count [(:variation/gene variation)
                (:variation/predicted-cds variation)
                (:variation/transcript variation)
                (:variation/pseudogene variation)
                (:variation/feature variation)
                (:variation/interactor variation)])))

(defn- create-fragment [peptide position]
  (str (- position 19)
       "..."
       (subs peptide (- position 20) 19)
       " "
       "<b>" (subs peptide (- position 1) 1) "</b>"
       " "
       (subs peptide position 20)
       "..."
       (+ position 19)))

(defn- do-markup [peptide var-start variation mutation-type]
  (let [style-map {:utr "FGCOLOR gray"
                   :cds0 "BGCOLOR yellow"
                   :cds1 "BGCOLOR orange"
                   :space " "
                   :unknown_mutation "background-color:#FF8080; text-transform:uppercase;"
                   :tandem_duplication "background-color:#FF8080; text-transform:uppercase;"
                   :substitution "background-color:#FF8080; text-transform:uppercase;"
                   :deletion "background-color:#FF8080; text-transform:uppercase;"
                   :insertion "background-color:#FF8080; text-transform:uppercase;"
                   :flank (if (= mutation-type "Insertion")
                            "background-color:yellow;font-weight:bold;text-transform:uppercase;"
                            "background-color:yellow")
                   :deletion_with_insertion "background-color: #FF8080; text-transform:uppercase;"}
        var-stop (if (= (count variation) 0)
                   (+ var-start 1)
                   (+ var-start (count variation)))
        sequence (if (= (count variation) 0)
                   (str (subs peptide 0 var-start) "-" (subs peptide (+ var-start (count variation))))
                   peptide)
        ]
    nil))

;test WBVar01112111 WBVar00601206
(defn features-affected [variation]
  {:data (pace-utils/vmap
           "Clone"
           (some->> (:sequence/clone
                      (:variation/mapping-target variation))
                    (map (fn [c]
                           (conj
                             (pack-obj c)
                             {:fstart nil
                              :fstop nil
                              :abs_start nil
                              :abs_stop nil
                              :start nil
                              :stop nil
                              :item (pack-obj c)}
                           ))
                    ))

           "Chromosome"
           (some->> (:variation/map variation)
                    (map :variation.map/map)
                    (map (fn [m]
                           (conj
                             (pack-obj m)
                             {:item (pack-obj m)
                              :fstart nil
                              :fstop nil
                              :abs_start nil
                              :abs_stop nil
                              :start nil
                              :stop nil
                              }
                             )

                           ) )
                    
                    )
           ; commenting out just to save space in output
;           "Gene"
;           (some->> (:variation/gene variation)
;                (map-indexed
;                  (fn [idx gh]
;                    (let [gene (:variation.gene/gene gh)
;                          obj (pack-obj gene)]
;                      {:entry (+ idx 1)
;                       :item obj
;                       :id (:gene/id gene)
;                       :label (:label obj)
;                       :class "gene"
;                       :taxonomy (:taxonomy obj)}))))

           "Predicted_CDS"
           (some->> (:variation/predicted-cds variation)
                (map
                  (fn [predicted-cds-holder]
                    (pace-utils/vmap
                      :protein_effects
                      (pace-utils/vmap
                        "Silent"
                        (if-let [cdshs (:molecular-change/missense predicted-cds-holder)]
                          (for [cdsh cdshs :let [position (first cdsh)
                                                 description (second cdsh)
                                                 cds (:variation.predicted-cds/cds cdsh)]]
                            (if-let [wt-protein (:cds/corresponding-protein cdsh)]
                              (if-let  [wt-peptide (:protein/peptide wt-protein)]
                                (let [formatted-wt-peptide (str/replace wt-peptide #"[^>|\n]" "")
                                      [wt-aa mut_aa] (re-seq #"(?s) to (?s)" description)
                                      mut-peptide (str/join
                                                    (assoc
                                                      (vec formatted-wt-peptide) (- position 1) mut_aa))
                                      wt-protein-fragment (create-fragment wt-peptide position)
                                      mut-protein-fragment (create-fragment mut-peptide position)
                                      ]
                                  {:wildtype_conceptual_translation nil
                                   :mutant_conceptual_translation nil
                                   }))))))

                      :location_effects nil
                      ))))
;
;            "Transcript"
;            (some->> (:variation/transcript variation)
;                     (map (fn [h]
;                            (let [t (:variation.transcript/transcript h)]
;                              (when-let  [refseqobj  (sequence-fns/genomic-obj t)]
;                            (conj
;                              (pack-obj t)
;                              {:item (pack-obj t)
;                               :fstart (:start refseqobj)
;                               :fstop (:stop refseqobj)
;                               :start nil
;                               :abs_stop nil
;                               :location_effects {:UTR_5 (let [fpu (:molecular-change/five-prime-utr h)
;                                                               ev (obj/get-evidence fpu)]
;                                                           {:evidence_type (some->> ev vals flatten first)
;                                                            :evidence (some->> ev keys first)})
;                                                  :UTR_3 (let [fpu (:molecular-change/three-prime-utr h)
;                                                               ev (obj/get-evidence fpu)]
;                                                           {:evidence_type (some->> ev vals flatten first)
;                                                            :evidence (some->> ev keys first)})}}))))))
;
;            "Pseudogene"
;            (some->> (:variation/pseudogene variation)
;                     (map :variation.pseudogene/pseudogene)
;                     (map (fn [pseudogene]
;                            (when-let [refseqobj (sequence-fns/genomic-obj pseudogene)]
;                              (conj
;                                (pack-obj pseudogene)
;                                {:fstart (:start refseqobj)
;                                 :fstop (:stop refseqobj)}
;                                {:item (pack-obj pseudogene)
;                                 }
;                                )
;                              ))))
           )
   :description "genomic features affected by this variation"})

(defn cgh-deleted-probes [variation]
  {:data nil
   :description "probes used for CGH of deletion alleles"})

(defn cgh-flanking-probes [variation]
  {:data nil
   :desciption "probes used for CGH of deletion alleles"})

(defn polymorphism-assays [variation]
  {:data nil
   :description "experimental assays for detecting this polymorphism"})

(defn affects-splice-site [variation]
  {:data {:donor nil
          :acceptor nil}
   :description "Affects splice site"})

(defn polymorphism-status [variation]; WBVar00116162 is predicted
  {:data (if (contains? variation :variation/confirmed-snp)
           "confirmed"
           (if (or (contains? variation :variation/snp)
                   (or (contains? variation :variation/reference-strain-digest)
                       (contains? variation :variation/transposon-insertion)))
             "predicted"))
   :description "experimental status of this polymorphism"})

(defn- pack-nulcleotide-change-obj [type-str wildtype mutant]
  {:type type-str
   :wildtype wildtype
   :mutant mutant
   :wildtype-label "wildtype"
   :mutant-label "variant"})

(defn- reverse-complement [dna]
  (str/replace
    (str/reverse dna)
    #"A|C|G|T|a|c|g|t"
    {"A" "T"
     "C" "G"
     "G" "C"
     "T" "A"
     "a" "t"
     "c" "g"
     "g" "c"
     "t" "a"}))

(defn- variation-features [variation]
  (if-let  [species-name (->> variation :variation/species :species/id)]
    (let  [g-species (generic-functions/xform-species-name species-name)
           sequence-database (seqdb/get-default-sequence-database g-species)
           db-spec ((keyword sequence-database) seqdb/sequence-dbs)
           variation-id (:variation/id variation)]
      (if sequence-database
        (seqdb/variation-features db-spec variation-id)))))

(defn- compile-nucleotide-changes [variation] ;WBVar00116162 substitution
  (remove nil?
          [
           (if-let [insertion (:variation/insertion variation)]
             (if-let [tis (:variation/transposon-insertion variation)]
               (for [ti tis] (keys ti)))
             ;              (pack-nucleotide-change-obj "insertion")
             )
           ;  )
           (if-let [deletion (:variation/deletion variation)]
             nil
             )
           (if-let [substitution (:variation/substitution variation)]
             (if-let [ref-allele (:variation.substitution/ref substitution)]
               (let [fs (:variation/flanking-sequences variation)
                     five-prime (:variation.flanking-sequences/five-prime fs)
                     three-prime (:variation.flanking-sequences/three-prime fs)
                     ]
                 five-prime)
 ;              (let [features (variation-features variation)]
;                 (println (str (seq (:object (first features)))))
;                 (if-let [feature (first features)]
;                   (let [wt (:variation.substitution/alt substitution)
;                         plus-strand-dna (:object feature)
;                         uc-plus-strand-dna (do (println plus-strand-dna)
;                                                (str/upper-case plus-strand-dna))]
;                     (if (not= (str/upper-case wt) uc-plus-strand-dna)
;                       (let [rc-wt (reverse-complement wt)]
;                         (println rc-wt)
;                         (println uc-plus-strand-dna)
;                         (if (= (str/upper-case rc-wt) uc-plus-strand-dna)
;                           (pack-nulcleotide-change-obj )
;                           {:wt rc-wt
;                            :dbid (:db/id variation)
;                            :mut (reverse-complement mut)}))))))
;)
              )) ]))


(defn nucleotide-change [variation]
  {:data (compile-nucleotide-changes variation)
   :d (:db/id variation)
   :description "raw nucleotide changes for this variation"})

;tested with WBVar00101112
(defn reference-strain [variation]
  {:data (if-let [vshs (:variation/strain variation)]
           (for [vsh vshs
                 :let [strain (:variation.strain/strain vsh)]]
             (pack-obj strain)))
   :description "strains that this variant has been observed in"})

(defn causes-frameshift [variaition]
  {:data nil
   :description "A variation that alters the reading frame"})

(defn sequencing-status [variation]
  {:data (if-let [seqstatus (:variation/seqstatus variation)]
           (name seqstatus))
   :description "sequencing status of the variation"})

(def widget
  {:name generic/name-field
;   :polymorphism_type polymorphism-type
;   :amino_acid_change amino-acid-change
;   :detection_method detection-method
;   :deletion_verification deletion-verification
;   :context context
;   :flanking_pcr_products flanking-pcr-products
;   :variation_type variation-type
;   :features_affected features-affected
;   :cgh_deleted_probes cgh-deleted-probes
;   :cgh_flanking_probes cgh-flanking-probes
;   :polymorphism_assays polymorphism-assays
;   :affects_splice_site affects-splice-site
;   :polymorphism_status polymorphism-status
   :nucleotide_change nucleotide-change
;   :reference_strain reference-strain
;   :causes_frameshift causes-frameshift
   :sequencing_status sequencing-status})
