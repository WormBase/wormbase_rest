(ns rest-api.classes.pcr-product.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn source [p]
  {:data (:clone.database/accession
           (first
             (:clone/database
               (first
                 (:pcr-product/clone p)))))
   :description "MRC geneservice reagent"})

(defn overlaps-cds [p]
  {:data (when-let [cdss (:cds/_corresponding-pcr-product p)]
           (for [cds cdss] (pack-obj cds)))
   :description "CDSs that this PCR product overlaps"})

(defn canonical-parent [p]
  {:daat (when-let [parent-sequence (:locatable/parent p)]
           (pack-obj parent-sequence))
   :description "Canonical parent of this PCR product"})

(defn on-orfeome-project [p]
  {:data (second (re-matches #"^mv_(.*)" (:pcr-product/id p)))
   :description "Finding this PCR product on the ORFeome project"})

(defn overlapping-genes [p]
  {:data (when-let [cdss (:cds/_corresponding-pcr-product p)]
           (not-empty
             (flatten
               (for [cds cdss
                     :let [hs (:gene.corresponding-cds/_cds cds)]]
                 (for [h hs
                       :let [gene (:gene/_corresponding-cds h)]]
                   (pack-obj gene))))))
   :description "Overlapping genes of this PCR product"})

(defn segment [p]
  {:data nil ; need to get from database
   :description "Sequence/segment data about this PCR product"})

(defn overlaps-variation [p]
  {:data (when-let [variations (:variation/_pcr-product p)]
           (for [variation variations]
             (pack-obj variation)))
   :description "Variations that this PCR product overlaps"})

(defn amplified [p]
  {:data (first (:pcr-product/amplified p))
   :description "Whether this PCR product is amplified"})

(defn oligos [p]
  {:data (when-let [ohs (:pcr-product/oligo p)]
           (for [oh ohs :let [oligo (:pcr-product.oligo/oligo oh)]]
             {:obj (pack-obj oligo)
              :sequence (:oligo/sequence oligo)}))
   :description "Oligos of thisPCR product"})

(defn pcr-products [p]
  {:data nil ; don't understand this one and can't find examples
   :description "PCR prodcuts associateed with this oligonucleotide"})

(defn in-sequences [p]
 {:data nil ; missing fields in datomic
  :description "Sequences containing this oligonucleotide"})

(defn microarray-results [p]
  {:data (when-let [ms (:microarray-results/_pcr-product p)]
           (into {}
                 (for [m ms]
                   {(:microarray-results/id m) (pack-obj m)})))
   :decription "Microarray results involving this PCR product"})

(defn overlaps-pseudogene [p]
  {:data nil
   :description "Pseudogenes that this PCR product overlaps"})

(defn overlaps-transcript [p]
  {:data (when-let [cdss (:cds/_corresponding-pcr-product p)]
           (not-empty
             (flatten
               (for [cds cdss
                     :let [hs (:transcript.corresponding-cds/_cds cds)]]
                 (for [h hs
                       :let [transcript (:transcript/_corresponding-cds h)]]
                   (pack-obj transcript))))))
   :description "Transcripts that this PCR product overlaps"})

(defn assay-conditions [p]
  {:data (when-let [text (:longtext/text
                           (first
                             (:pcr-product/assay-conditions p)))]
           (str/replace
             (str/replace text #"^\n+" "")
             #"\n" "<br>"))
   :description "Assay conditions for this PCR product"})

(defn snp-loci [p]
  {:data nil
   :description "SNP loci for this PCR product"})

(defn rnai [p]
  {:data (when-let [rs (:rnai/_pcr-product p)]
           (for [r rs] (pack-obj r)))
   :k (keys p)
   :d (:db/id p)
   :description "associated RNAi experiments"})

(def widget
  {:name generic/name-field
   :source source
   :overlaps_CDS overlaps-cds
   :canonical_parent canonical-parent
   :on_orfeome_project on-orfeome-project
   :overlapping_genes overlapping-genes
   :segment segment
   :overlaps_variation overlaps-variation
   :amplified amplified
   :remarks generic/remarks
   :oligos oligos
   :pcr_products pcr-products
   :laboratory generic/laboratory
   :in_sequences in-sequences
   :microarray_results microarray-results
   :overlaps_pseudogene overlaps-pseudogene
   :overlaps_transcript overlaps-transcript
   :assay_conditions assay-conditions
   :SNP_loci snp-loci
   :rnai rnai})
