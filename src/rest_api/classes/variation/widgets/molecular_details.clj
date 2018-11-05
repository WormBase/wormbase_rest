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

(defn str-insert
  "Insert c in string s at index i."
  [s c i]
  (str (subs s 0 i) c (subs s i)))

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
     "t" "a"
     "-" "-"}))

(defn- fetch-coords-in-feature [varrefseqobj object]
  (let [refseqobj (sequence-fns/genomic-obj object)]
    {:fstart (:start refseqobj)
     :fstop (:stop refseqobj)
     :start (+ 1  ;start and stop incorrect for predicted-cds of WBVar00601206
               (- (:start varrefseqobj)
                  (:start refseqobj)))
     :stop (+ 1
              (- (:stop varrefseqobj)
                 (:start refseqobj)))
     :abs_start (:start varrefseqobj)
     :abs_stop (:stop varrefseqobj)
     :item (pack-obj object)}))

(defn- retrieve-molecular-changes [object]
  (let [do-translation (if (or
                             (contains? object :molecular-change/missense)
                             (contains? object :molecular-change/nonsense))
                         true
                         false)
        protein-effects nil
        molecular-effects "todo"
        ]
    [protein-effects molecular-effects do-translation]))

(defn- get-feature-affected-evidence [feature]
  (let [ev (obj/get-evidence feature)]
    (not-empty
      (pace-utils/vmap
        :evidence_type
        (some->> ev vals flatten first)

        :evidence
        (some->> ev keys first)))))

(defn- mutant-conceptual-translation [pseq position from to]
  (str
    (subs
      pseq
      0
      (- position 1))
    to
    (subs
      pseq
      (- (+
          position
          (count from))
         1))))

(defn- get-missense-obj [predicted-cds-holder]
  (when-let [m (first (:molecular-change/missense predicted-cds-holder))]
    (let [cds (:variation.predicted-cds/cds predicted-cds-holder)
          description (:molecular-change.missense/text m)
          position (:molecular-change.missense/int m)
          [full from to] (re-matches #"(.*)\sto\s(.*)" description)
          protein (:cds.corresponding-protein/protein
                    (:cds/corresponding-protein cds))
          peptide (:protein.peptide/peptide
                    (:protein/peptide protein))
          pseq (:peptide/sequence peptide)]
      (conj
        {:aa_change (str from position to)
         :position position
         :from from
         :to to
         :description description
         :protein (pack-obj protein)
         :peptide (pack-obj peptide)
         :wildtype_conceptual_translation pseq ; eg. WBVar00274871
         :mutant_conceptual_translation (mutant-conceptual-translation pseq position from to)}
        (get-feature-affected-evidence m)))))

(defn polymorphism-type [variation]
  {:data (if (contains? variation :variation/snp)
           (if (contains? variation :variation/reference-strain-digest)
             "SNP and RFLP"
             "SNP")
           (when-let [insertion-str (:transposon-family/id
                                      (first
                                        (:variation/transposon-insertion variation)))]
             (str insertion-str " transposon insertion" )))
   :description "the general class of this polymorphism"})

(defn amino-acid-change [variation] ; e.g. WBVar00271007
  {:data (some->> (:variation/predicted-cds variation)
                  (map (fn [pcdsh]
                         {:amino_acid_change (:aa_change (get-missense-obj pcdsh))
                          :transcript (pack-obj (:variation.predicted-cds/cds pcdsh))})))
   :description "amino acid changes for this variation, if appropriate"})

(defn detection-method [variation] ; WBVar00601206
  {:data (first (:variation/detection-method variation))
   :description "detection method for polymorphism, typically via sequencing or restriction digest."})

(defn deletion-verification [variation]; e.g. WBVar00278357
  {:data (some->> (:variation/deletion-verification variation)
                  (map (fn [h]
                         {:text (:variation.deletion-verification/text h)
                          :evidence (obj/get-evidence h)})))
   :description "the method used to verify deletion alleles"})

;tested with: WBVar00750883,
(defn context [variation]
  {:data (let [max-seqlen 1000000
               padding 250
               flank-length 15
               refseqobj (sequence-fns/genomic-obj variation)

               seq-length (+ 1
                             (- (:stop refseqobj)
                                (:start refseqobj)))

               placeholder (when (> seq-length 1000000)
                             seq-length)

               wildtype-sequence (when (nil? placeholder)
                                   (sequence-fns/get-sequence
                                     (conj
                                       refseqobj
                                       {:start (- (:start refseqobj) padding)
                                        :stop (+ padding (:stop refseqobj))})))

               wildtype-full-length (+ (* 2 padding)
                                       seq-length)

               length-change (if-let [insertion (:variation/insertion variation)]
                               (or (if-let [insertion-str (:variation.insertion/text insertion)]
                                     (count insertion-str)
                                     (when (contains? variation :variation/transposon-insertion)
                                       (- (count (:transposon-family/id
                                                   (first (:variation.transpon-insertion variation))))
                                          1))))
                               (if-let [deletion (:variation/deletion variation)]
                                 (- 0 seq-length)
                                 (if-let [substitution (:variation/substitution variation)]
                                   (- (count (:variation.substitution/alt substitution))
                                      (count (:variation.substitution/ref substitution)))
                                   (when (contains? variation :variation/tandem-duplication) 0))))

               wildtype-positive (when (nil? placeholder)
                                   {:sequence (if (or
                                                    (contains? variation :variation/insertion)
                                                    (contains? variation :variation/transpson-insertion)
                                                    (contains? variation :variation/tandem-duplication))
                                                      (str-insert wildtype-sequence
                                                                  "-"
                                                                  padding)
                                                (apply str (map-indexed (fn [index val]
                                                                          (if (and (>= index (+ 1 padding))
                                                                                   (<= index (+ padding
                                                                                                seq-length)))
                                                                            (str/upper-case val)
                                                                            val))
                                                                        wildtype-sequence)))
                                    :features
                                    {:variation
                                     {:type "variation"
                                      :start (+ padding 1)
                                      :stop (+ padding
                                               seq-length)}
                                     :left-flank {:type "flank"
                                                  :start (- (+ padding 1) flank-length)
                                                  :stop padding}
                                     :right-flank {:type "flank"
                                                   :start (+ padding 1 seq-length)
                                                   :stop (+
                                                          padding
                                                          seq-length
                                                          flank-length)}}})

               wildtype-negative (when (nil? placeholder)
                                   {:sequence
                                    (reverse-complement (:sequence wildtype-positive))

                                    :features
                                    (:features wildtype-positive)})

               mutant-positive (when (nil? placeholder)
                                     {:sequence
                                      (cond
                                        (contains? variation :variation/substitution)
                                        (let [substitution (:variation/substitution variation)]
                                          (str/replace
                                            (:sequence wildtype-positive)
                                            (:variation.substitution/ref substitution)
                                            (:variation.substitution/alt substitution)))

                                        (contains? variation :variation/insertion)
                                        (let [insertion (:variation/insertion variation)
                                              insert-str (or (:variation.insertion/text insertion)
                                                             (:transposon-family/id
                                                               (first
                                                                 (:variation/transposon-insertion variation))))]
                                          (str/replace
                                            (:sequence wildtype-positive)
                                            #"\-+"
                                            insert-str))

                                        (contains? variation :variation/deletion)
                                        (str/replace
                                          (:sequence wildtype-positive)
                                          #"[A-Z]+"
                                          "")

                                        (contains? variation :variation/tandem-duplication)
                                        (:sequence wildtype-positive))

                                      :features
                                      {:variation
                                       {:type "variation"
                                        :start (:start (:variation (:features wildtype-positive)))
                                        :stop (+ (:stop (:variation (:features wildtype-positive)))
                                                 length-change)}

                                       :left-flank
                                       (:left-flank (:features wildtype-positive))

                                       :right-flank
                                       {:type "flank"
                                        :start (+ (:start (:right-flank (:features wildtype-positive)))
                                                  length-change)
                                        :stop (+ (:stop (:right-flank (:features wildtype-positive)))
                                                 length-change)}}})

                 mutant-negative (when (nil? placeholder)
                                   {:sequence
                                    (if-let [transposon-family (:transposon-family/id
                                                                 (first
                                                                   (:variation/transposon-insertion variation)))]
                                      (str/replace
                                        (reverse-complement (:sequence mutant-positive))
                                        (re-pattern (reverse-complement transposon-family))
                                        transposon-family)
                                      (reverse-complement (:sequence mutant-positive)))

                                    :features
                                    (:features mutant-positive)})

                 species (some->> (:species/assembly (:variation/species variation))
                                  (map (fn [assembly]
                                         (:strain/id (first (:sequence-collection/strain assembly))))))]
               (when-not (every? nil? [wildtype-positive placeholder])
                 (pace-utils/vmap
                   :wildtype (not-empty
                               (pace-utils/vmap
                                 :positive-strand wildtype-positive
                                 :negative-strand wildtype-negative))
                   :mutant (not-empty
                             (pace-utils/vmap
                               :positive-strand mutant-positive
                               :negative-strand mutant-negative
                               ))
                   :public-name (:variation/public-name variation)
                   :placeholder placeholder)))
   :description "wild type and variant sequences in genomic context"})

(defn flanking-pcr-products [variation] ; tested with WBVar00145789
  {:data (some->> (:variation/pcr-product variation)
                  (map (fn [p]
                        {(:pcr-product/id p) (pack-obj p)})))
   :description "PCR products that flank the variation"})

;test WBVar01112111
(defn variation-type [variation]
  (let [variation-type-map {:variation/engineeered-allele "Engineered allele"
                            :variation/allele"Allele"
                            :variation/snp "SNP"
                            :variation/confirmed-snp "Confirmed SNP"
                            :variation/reference-strain-digest "RFLP"
                            :variation/predicted-snp "Predicted SNP"
                            :variation/transposon-linsertion "Transposon Insertion"
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

;test WBVar01112111 WBVar00601206
(defn features-affected [variation]
  {:data (when-let [varrefseqobj (sequence-fns/genomic-obj variation)]
           (pace-utils/vmap
             "Clone" ;checked with WBVar00274017
             (when-let [s (:variation/mapping-target variation)]
               [(conj
                  (pack-obj s)
                  (fetch-coords-in-feature varrefseqobj s))])

             "Chromosome" ;tested with WBVar00274017
             (some->> (:variation/gene variation)
                      (first)
                      (:variation.gene/gene)
                      (:gene/interpolated-map-position)
                      (:gene.interpolated-map-position/map)
                      (pack-obj))

             "Gene" ;tested with WBVar00274017
             (some->> (:variation/gene variation)
                      (map-indexed
                        (fn [idx gh]
                          (let [gene (:variation.gene/gene gh)
                                obj (pack-obj gene)]
                            {:entry (+ idx 1)
                             :item obj
                             :id (:gene/id gene)
                             :label (:label obj)
                             :class "gene"
                             :taxonomy (:taxonomy obj)}))))

             "Predicted_CDS" ;tested with WBVar01112111
             (some->> (:variation/predicted-cds variation)
                      (map
                        (fn [predicted-cds-holder]
                          (let [cds (:variation.predicted-cds/cds predicted-cds-holder)]
                            (conj
                              (pack-obj cds)
                              (fetch-coords-in-feature varrefseqobj cds) ; appears to a discreptency. This code gives 2945
                              (pace-utils/vmap
                                :protein_effects
                                (not-empty
                                  (pace-utils/vmap
                                    "Silent" ;tested with WBVar01112111
                                    (some->> (:molecular-change/silent predicted-cds-holder)
                                             (map (fn [mc]
                                                    (conj
                                                      {:description (:molecular-change.silent/text mc)}
                                                      (get-feature-affected-evidence mc)))))

                                    "Missense" ; eg. WBVar00273293
                                    (get-missense-obj predicted-cds-holder)

                                    "Frameshift" ; e.g. WBVar00273213
                                    (when-let [fs (first (:molecular-change/frameshift predicted-cds-holder))]
                                      (conj
                                        {:description (:molecular-change.frameshift/text fs)}
                                        (get-feature-affected-evidence fs)))))

                                :location_effects
                                (not-empty
                                  (pace-utils/vmap
                                    "Coding_exon" ;tested with WBVar01112111
                                    (when-let [ce (:molecular-change/coding-exon predicted-cds-holder)]
                                      (get-feature-affected-evidence ce))

                                    "Intron" ;tested with WBVar00271172
                                    (when-let [i (:molecular-change/intron predicted-cds-holder)]
                                      (get-feature-affected-evidence i))))))))))

             "Transcript"
             (some->> (:variation/transcript variation)
                      (map (fn [h]
                             (let [t (:variation.transcript/transcript h)]
                               (when-let [refseqobj (sequence-fns/genomic-obj t)]
                                 (conj
                                   (pack-obj t)
                                   (fetch-coords-in-feature varrefseqobj t) ; start and stop incorrect when testing with WBVar00601206
                                   {:item
                                    (pack-obj t)

                                    :location_effects
                                    (not-empty
                                      (pace-utils/vmap
                                        :UTR_5
                                        (get-feature-affected-evidence
                                          (:molecular-change/five-prime-utr h))

                                        :UTR_3
                                        (get-feature-affected-evidence
                                          (:molecular-change/three-prime-utr h))))}))))))

             "Pseudogene" ;tested with WBVar00601206
             (some->> (:variation/pseudogene variation)
                      (map :variation.pseudogene/pseudogene)
                      (map (fn [pseudogene]
                             (when-let [refseqobj (sequence-fns/genomic-obj pseudogene)]
                               (conj
                                 (pack-obj pseudogene)
                                 (fetch-coords-in-feature varrefseqobj pseudogene)
                                 {:item (pack-obj pseudogene)})))))))
   :description "genomic features affected by this variation"})

(defn cgh-deleted-probes [variation] ; tested with WBVar00601206
  {:data (when-let [dp (:variation/cgh-deleted-probes variation)]
           {:left_flank (:variation.cgh-deleted-probes/text-a dp)
            :right_flank (:variation.cgh-deleted-probes/text-b dp)})
   :description "probes used for CGH of deletion alleles"})

(defn cgh-flanking-probes [variation] ; missing data in Datomic for WBVar00601206
  {:data (keys variation)
   :d (:db/id variation)
   :desciption "probes used for CGH of deletion alleles"})

(defn polymorphism-assays [variation]; WBVar00597552
  {:data  (some->> (:variation/pcr-product variation)
                   (map (fn [pcr]
                          {(:pcr-product/id pcr)
                           (let [ohs (:pcr-product/oligo pcr)]
                             (conj
                               (pack-obj pcr)
                               {:pcr_conditions nil ; from pcr-product/assay-conditions non found
                                :dna nil ;non found
                                :left_oligo (:oligo/sequence
                                              (:pcr-product.oligo/oligo
                                                (first ohs)))
                                :right_oligo (:oligo/sequence
                                               (:pcr-product.oligo/oligo
                                                 (second ohs)))}))})))
   :description "experimental assays for detecting this polymorphism"})

(defn affects-splice-site [variation] ; made from WBVar00750883. This was in Ace code but wasn't working and format slightly changed
  {:data (some->> (:variation/predicted-cds variation)
                  (map :molecular-change/splice-site)
                  (map (fn [mc]
                         {:value (name
                                   (:molecular-change.splice-site/value mc))
                          :text (:molecular-change.splice-site/text mc)})))
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
          [(conj
             {:mutant_label "variant"
              :wildtype_label "wild type"}
             (when-let [insertion (:variation/insertion variation)] ; tested with WBVar00269113
               {:mutation (if-let [mut (or (:transposon-family/id
                                             (first
                                               (:variation/transposon-insertion variation)))
                                           (:variation/d variation))] ; need to check method
                            mut
                            (:variation.insertion/text insertion))
                :type "Insertion"
                :wildtype ""})
             (when-let [deletion (:variation/deletion variation)] ;eg WBVar00601206
               (if (contains? variation :variation/cgh-deleted-probes)
                 {:type "Definition Deletion" ; eg WBVar00601206
                  :db (:db/id variation)
                  :wildtype nil ; need to figure out how to get full sequence
                  }
                 {:type "Deletion" ; eg WBVar00274723
                  :wildtype (when-let [deletion (:variation.deletion/text
                                                  (:variation/deletion variation))]
                              (str/lower-case deletion))}))
             (when-let [substitution (:variation/substitution variation)] ; e.g. tested with WBVar00274017
               {:mutant (:variation.substitution/alt substitution)
                :wildtype (:variation.substitution/ref substitution)
                :type "Substitution"}))]))


(defn nucleotide-change [variation]
  {:data (compile-nucleotide-changes variation)
   :description "raw nucleotide changes for this variation"})

;tested with WBVar00101112
(defn reference-strain [variation]
  {:data (some->> (:variation/strain variation)
                  (map :variation.strain/strain)
                  (map pack-obj)
                  (sort-by :label))
   :description "strains that this variant has been observed in"})

(defn causes-frameshift [variation]; e.g. WBVar01943248 this does not work on Ace version
  {:data (some->> (:variation/predicted-cds variation)
                  (first)
                  (:molecular-change/frameshift)
                  (first)
                  (:molecular-change.frameshift/text))
   :description "A variation that alters the reading frame"})

(defn sequencing-status [variation]
  {:data (when-let [seqstatus (:variation/seqstatus variation)]
           (name seqstatus))
   :description "sequencing status of the variation"})

(def widget
  {:name generic/name-field
   :polymorphism_type polymorphism-type
   :amino_acid_change amino-acid-change
   :detection_method detection-method
   :deletion_verification deletion-verification
   :context context
   :flanking_pcr_products flanking-pcr-products
   :variation_type variation-type
   :features_affected features-affected
   :cgh_deleted_probes cgh-deleted-probes
   :cgh_flanking_probes cgh-flanking-probes
   :polymorphism_assays polymorphism-assays
   :affects_splice_site affects-splice-site
   :polymorphism_status polymorphism-status
   :nucleotide_change nucleotide-change
   :reference_strain reference-strain
   :causes_frameshift causes-frameshift
   :sequencing_status sequencing-status})
