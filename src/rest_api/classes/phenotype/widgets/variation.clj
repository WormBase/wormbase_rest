(ns rest-api.classes.phenotype.widgets.variation
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn variation-info [p obs]
  (when-let [holders (if obs
                       (:variation.phenotype/_phenotype p)
                       (:variation.phenotype-not-observed/_phenotype p))]
    (for [holder holders
          :let [variation (if obs
                       (:variation/_phenotype holder)
                       (:variation/_phenotype-not-observed holder))]]
      {:variation (pack-obj variation)
       :gene (when-let [ghs (:variation/gene variation)]
               (for [gh ghs
                     :let [gene (:variation.gene/gene gh)]]
                 (pack-obj gene)))
       :species (when-let [species (:variation/species variation)]
                  (pack-obj species))
       :type (cond
               (:variation/allele variation)
               "Allele"

               (:variation/confirmed-snp variation)
               "Confirmed SNP"

               (:variation/snp variation)
                 "SNP"

               (:variation/predicted-snp variation)
               "Predicted SNP"

               (:variation/transposon-insertion variation)
               "Transposon Insertion"

               (:variation/natural-variant variation)
               "Natural Variant")

               (or (:variation/polymorphic-strain-digest variation)
                   (:variation/reference-strain-digest variation))
               "RFLP"})))

(defn variation [p]
  {:data (variation-info p true)
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(defn variation-not [p]
  {:data (variation-info p false)
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(def widget
  {:variation variation
   :variation_not variation-not
   :name generic/name-field})
