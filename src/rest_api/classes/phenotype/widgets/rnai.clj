(ns rest-api.classes.phenotype.widgets.rnai
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn rnai-info [p obs]
  (when-let [holders (if obs
                       (:rnai.phenotype/_phenotype p)
                       (:rnai.phenotype-not-observed/_phenotype p))]
    (for [holder holders
          :let [rnai (if obs
                       (:rnai/_phenotype holder)
                       (:rnai/_phenotype-not-observed holder))]]
      {:rnai (pack-obj rnai)
       :sequence (when-let [ss (:rnai/sequence rnai)] ; in Perl API this single instance not array
                   (for [s ss]
                     (pack-obj s)))
       :species (when-let [species (:rnai/species rnai)]
                  (pack-obj species))
       :genotype (:rnai/genotype rnai)
       :treatment (:rnai/treatment rnai)
       :strain (when-let [strain (:rnai/strain rnai)]
                 (pack-obj strain))})))

(defn rnai [p]
  {:data (rnai-info p true)
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(defn rnai-not [p]
  {:data (rnai-info p false)
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(def widget
  {:rnai rnai
   :rnai_not rnai-not
   :name generic/name-field})
