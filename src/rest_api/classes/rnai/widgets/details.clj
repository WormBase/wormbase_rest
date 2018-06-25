(ns rest-api.classes.rnai.widgets.details
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.sequence.core :as sequence-fns]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn delivered-by [r]
  {:data (when-let [by (:rnai/delivered-by r)]
           (obj/humanize-ident (name by)))
   :description "how the RNAi was delivered to the animal"})

(defn assay [r]
  {:data (if (contains? r :rnai/pcr-product)
           "PCR product"
           "Sequence")
   :description "assay performed on the rnai"})

(defn sqnc [r]
  {:data (if-let [hs (:rnai/dna-text r)]
           (some->> hs
                    (map (fn [h]
                           (let [s (:rnai.dna-text/sequence h)]
                             {:length (count s)
                              :sequence s
                              :header (:rnai.dna-text/name s)}))))
          (some->> (:rnai/pcr-product r)
                  (map (fn [obj]
                         (when-let [refseqobj (sequence-fns/genomic-obj obj)]
                           (when-let [refseq (sequence-fns/get-sequence refseqobj)]
                             {:length (count refseq)
                              :sequence refseq
                              :header (:pcr-product/id obj)}))))
                  (remove nil?)))
   :description "rnai sequence"})

(defn genotype [r]
  {:data (:rnai/genotype r)
   :description "genotype of rnai strain"})

(defn treatment [r]
  {:data (:rnai/treatment r)
   :description "experimental conditions for rnai analysis"})

(defn strain [r]
  {:data (when-let [strain (:rnai/strain r)]
           (pack-obj strain))
   :description "strain of origin of rnai"})

(defn reagent [r]
  {:data (some->> (:rnai/pcr-product r)
                  (map (fn [pcr]
                         {:reagent (pack-obj pcr)
                          :mrc_id (some->> (:pcr-product/clone pcr)
                                           (map :clone/database)
                                           (map (fn [dhs]
                                                  (some->> dhs
                                                           (map (fn [dh]
                                                                  (when (= (:database/id (:clone.database/database dh))
                                                                           "Source_BioScience")
                                                                      (:clone.database/accession dh)))))))
                                           (flatten)
                                           (remove nil?)
                                           (first))})))
   :description "PCR products used to generate this RNAi"})

(defn life-stage [r]
  {:data nil ; non found in database
   :description "life stage in which rnai is observed"})

(def widget
  {:name generic/name-field
   :delivered_by delivered-by
   :assay assay
   :sequence sqnc
   :genotype genotype
   :treatment treatment
   :strain strain
   :reagent reagent
   :life_stage life-stage})
