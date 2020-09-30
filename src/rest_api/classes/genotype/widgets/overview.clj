(ns rest-api.classes.genotype.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))


(defn synonyms [genotype]
  {:data (->> (:genotype/genotype-synonym genotype)
              (sort)
              (seq))
   :description "a synonym for the genotype"})

(defn genotype-components [genotype]
  {:data (->> (concat (:genotype/variation genotype)
                      (:genotype/transgene genotype)
                      (:genotype/rearrangement genotype)
                      (list (:genotype/other-component genotype)))
              (map pack-obj)
              (seq))})

(defn models-diseases [genotype]
  {:data (->> (:genotype/models-disease genotype)
              (map pack-obj)
              (seq))})

(def widget
  {:name generic/name-field
   :synonyms synonyms
   :taxonomy generic/taxonomy
   :genotype_components genotype-components
   :models_diseases models-diseases
   })
