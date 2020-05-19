(ns rest-api.classes.phenotype.widgets.rnai
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn rnai-info [p rnais]
  (some->> (seq rnais)
           (map
             (fn [rnai]
               {:rnai (pack-obj rnai)
                :sequence (when-let [ss (:rnai/sequence rnai)]
                            (map pack-obj ss))
                :species (when-let [species (:rnai/species rnai)]
                           (pack-obj species))
                :genotype (:rnai/genotype rnai)
                :treatment (:rnai/treatment rnai)
                :strain (when-let [strain (:rnai/strain rnai)]
                          (pack-obj strain))}))))

(defn rnai [p]
  (let [rnais (map :rnai/_phenotype (:rnai.phenotype/_phenotype p))]
    {:data (rnai-info p rnais)
     :description (str "The name and WormBase internal ID of " (:db/id p))}))

(defn rnai-not [p]
  (let [rnais (map :rnai/_phenotype-not-observed (:rnai.phenotype-not-observed/_phenotype p))
        count-rnais (count rnais)]
    {:data (when (< count-rnais 3000)
             (rnai-info p rnais))
     :count count-rnais
     :description (str "The name and WormBase internal ID of " (:db/id p))}))

(def widget
  {:rnai rnai
   :rnai_not rnai-not
   :name generic/name-field})
