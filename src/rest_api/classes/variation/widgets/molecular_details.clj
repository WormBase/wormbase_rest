(ns rest-api.classes.variation.widgets.molecular-details
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.db.sequence :as seqdb]
    [rest-api.classes.generic :as global-generic]
    [rest-api.classes.gene.sequence :as gene-sequence]
    [rest-api.classes.variation.generic :as variation-generic]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn polymorphism-type [variation]
  {:data nil
   :description "the general class of this polymorphism"})

(defn amino-acid-change [variation]
  {:data nil
   :description "amino acid changes for this variation, if appropriate"})

(defn detection-method [variation]
 {:data nil
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
                            :variation/predicted-snp "Predicted SNP"
                            :variation/transposon-insertion "Transposon Insertion"
                            :variation/natural-variant "Natural Variant"}
        type-of-mutation-map {:variation/substitution "Substitution"
                              :variation/insertion "Insertion"
                              :variation/detetion "Deletion"
                              :variation/tandem-duplication "Tandem Duplication"}]
    {:data {:pysical_class (cond
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
                                          (true? ((first %) variation))
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

(defn- do_markup [peptide var-start variation mutation-type]
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
                 ]                                                     )
    nil                                                                                                                                                                           )

;test WBVar01112111/
(defn features-affected [variation]
  {:data (pace-utils/vmap
           "Clone"
           nil

           "Chromosome"
           nil

           "Gene"
           (->>
             (:variation/gene variation)
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

           "Predicted_CDS"
           (->>
             (:variation/predicted-cds variation)
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
                 )))
             )

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

(defn polymorphism-status [variation]
  {:data nil
   :description "experimental status of this polymorphism"})

(defn- pack-nulcleotide-change-obj [type-str wildtype mutant]
  {:type type-str
   :wildtype wildtype
   :mutant mutant
   :wildtype-label "wildtype"
   :mutant-label "variant"}
  )

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

(defn- variation-features  [variation]
  (if-let  [species-name  (->> variation :variation/species :species/id)]
    (let  [g-species  (global-generic/xform-species-name species-name)
           sequence-database  (seqdb/get-default-sequence-database g-species)
           db-spec  ((keyword sequence-database) seqdb/sequence-dbs)
           variation-id  (:variation/id variation)]
      (if sequence-database
        (do (println db-spec) (println variation-id)
        (seqdb/variation-features db-spec variation-id))))))

(defn- compile-nucleotide-changes [variation]
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
             (if-let [mut (:variation.substitution/ref substitution)]
               (let [features (variation-features variation)]
                 (println (str (seq (:object (first features)))))
                 (if-let [feature (first features)]
                   (let [wt (:variation.substitution/alt substitution)
                         plus-strand-dna (:object feature)
                         uc-plus-strand-dna (do (println plus-strand-dna)
                                                (str/upper-case plus-strand-dna))]
                     (if (not= (str/upper-case wt) uc-plus-strand-dna)
                       (let [rc-wt (reverse-complement wt)]
                         (println rc-wt)
                         (println uc-plus-strand-dna)
                         (if (= (str/upper-case rc-wt) uc-plus-strand-dna)
                           (pack-nulcleotide-change-obj )
                         {:wt rc-wt
                          :dbid (:db/id variation)
                          :mut (reverse-complement mut)}))))))
               
               ))]))


(defn nucleotide-change [variation]
  {:data  nil ;(compile-nucleotide-changes variation)
   :keys (keys variation)
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
  {:name variation-generic/name-field
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
