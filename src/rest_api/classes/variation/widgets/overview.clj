(ns rest-api.classes.variation.widgets.overview
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.classes.variation.generic :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn taxonomy [variation]
  {:data (if-let [sid(:species/id (:variation/species variation))]
             (let [[genus species] (str/split sid #" ")]
               {:genus genus
                :species species}))
   :description "the genus and species of the current object"})

; This needs to be worked on
(defn corresponding-gene [variation]
  (let [db  (d/entity-db variation)
        genes (seq (map :variation.gene/gene (:variation/gene variation)))]
    {:data (if (nil? genes)
             (for [gene genes]
               (pack-obj gene)))
     :description "gene in which this variation is found (if any)"}))

;need to find and instance where get-evidence works for variation
(defn evidence [variation]
  {:data (if-let [evidence (obj/get-evidence variation)]
           {:text ""
            :evidence evidence})
   :description "Evidence for this Variation"})

(defn pysical-class [variation]
  (cond
     (:variation/substitution variation) "Substitution"
     (:variation/insertion variation) "Insertion"
     (:variation/deletion variation) "Deletion"
     (:variation/tandem-duplicatino variation) "Tandem Duplication"
     (:variation/pcr-product variation) "PCR Product"))

(defn- variation-type-data [variation]
  (let [types-str-map {:snp               "SNP"
                       :predicted-snp     "Predicted SNP"
                       :confirmed-snp     "Confirmed SNP"
                       :natural-variant   "Natural Variant"
                       :allele            "Allele"
                       :engineered-allele "Engineered Allele"}
        data-variants  {:snp (:variation/snp variation)
                        :predicted-snp (:variation/predicted-snp variation)
                        :confirmed-snp (:variation/confirmed-snp variation)
                        :natural-variant (:variation/natural-variant variation)
                        :allele  (:variation/allele variation)
                        :engineered-allele (:variation/engineered-allele variation)}
        general-class  (reduce-kv
                         (fn  [acc k v]
                           (if  (= v true)
                             (conj acc (types-str-map k))
                             acc))
                         #{} data-variants)]

    {:physical_class (pysical-class variation)
     :general_class general-class}))

(defn variation-type [variation]
  {:data (variation-type-data variation)
   :description "the general type of the variation"})

(defn remarks [variation]
  (let [data (->>
               (:variation/remark variation)
               (map  (fn  [rem]
                       {:text  (:variation.remark/text rem)
                        :evidence  (obj/get-evidence rem)}))
               (seq))]
  {:data data
   :description "curatorial remarks for the Variation"}))

(defn other-names [variation]
  {:data (if-let [other-names (:variation/other-name variation)]
           (seq other-names))
   :description (format "other names that have been used to refer to %s" (:variation/id variation))})

(def widget
  {:name               generic/name-field
   :taxonomy           taxonomy
   :corresponding_gene corresponding-gene
   :evidence           evidence
   :variation_type     variation-type
   :remarks            remarks
   :other_names        other-names})
