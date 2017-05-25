(ns rest-api.classes.pcr-product.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn source [po]
  {:data nil
   :description "MRC geneservice reagent"})

(defn overlaps-cds [po]
  {:data nil
   :description "CDSs that this PCR product overlaps"})

(defn canonical-parent [po]
  {:daat nil
   :description "Canonical parent of this PCR product"})

(defn on-orfeome-project [po]
  {:data nil
   :description "Finding this PCR product on the ORFeome project"})

(defn overlapping-genes [po]
  {:data nil
   :description "Overlapping genes of this PCR product"})

(defn segment [po]
  {:data nil
   :description "Sequence/segment data about this PCR product"})

(defn overlaps-variation [po]
  {:data nil
   :description "Variations that this PCR product overlaps"})

(defn amplified [po]
  {:data nil
   :description "Whether this PCR product is amplified"})

(defn oligos [po]
  {:data (when-let [ohs (:pcr-product/oligo po)]
           (for [oh ohs :let [oligo (:pcr-product.oligo/oligo oh)]]
             {:obj (pack-obj oligo)
              :sequence (:oligo/sequence oligo)}))
   :description "Oligos of thisPCR product"})

(defn pcr-products [po]
  {:data nil
   :description "PCR prodcuts associateed with this oligonucleotide"})

(defn laboratory [po]
  {:data nil
   :description "the laboratory where the PCR_product was isolated, created, or named"})

(defn is-sequences [po]
 {:data nil
  :description "Sequences containing this oligonucleotide"})

(defn microarray-results [po]
  {:data nil
   :description "Microarray results involving this PCR product"})

(defn overlaps-pseudogene [po]
  {:data nil
   :description "Pseudogenes that this PCR product overlaps"})

(defn overlaps-transcript [po]
  {:data nil
   :description "Transcripts that this PCR product overlaps"})

(defn assay-conditions [po]
  {:data nil
   :description "Assay conditions for this PCR product"})

(defn snp-loci [po]
  {:data nil
   :description "SNP loci for this PCR product"})

(defn rnai [po]
  {:data nil
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
   :laboratory laboratory
   :is_sequences is-sequences
   :microarray_results microarray-results
   :overlaps_pseudogene overlaps-pseudogene
   :overlaps_transcript overlaps-transcript
   :assay_conditions assay-conditions
   :SNP_loci snp-loci
   :rnai rnai})
