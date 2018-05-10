(ns rest-api.classes.rnai.widgets.details
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn delivered-by [r]
  {:data (when-let [by (:rnai/delivered-by r)]
           (obj/humanize-ident (name by)))
   :d (:db/id r)
   :description "how the RNAi was delivered to the animal"})

(defn assay [r]
  {:data (if (contains? r :rnai/pcr-product)
           "PCR product"
           "Sequence")
   :description "assay performed on the rnai"})

(defn sqnc [r]
  {:data nil
   :definition "rnai sequence"})

(defn genotype [r]
  {:data nil
   :description "genotype of rnai strain"})

(defn treatment [r]
  {:data nil
   :description "experimental conditions for rnai analysis"})

(defn strain [r]
  {:data nil
   :description "strain of origin of rnai"})

(defn reagent [r]
  {:data (some->> (:rnai/pcr-product r)
                  (map (fn [pcr]
                         {:reagent (pack-obj pcr)
                          :mrc_id (some->> (:pcr-product/clone pcr)
                                           (map :db/id))}))) ; need to find database
   :description "PCR products used to generate this RNAi"})

(defn life-stage [r]
  {:data nil
   :description "PCR product used to generate this RNAi"})

(def widget
  {:name generic/name-field
   :delivered_by delivered-by
   :assays assay
   :sequence sqnc
   :genotype genotype
   :treatment treatment
   :strain strain
   :reagent reagent
   :life_stage life-stage})
