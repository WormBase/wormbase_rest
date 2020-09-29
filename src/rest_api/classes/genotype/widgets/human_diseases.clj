(ns rest-api.classes.genotype.widgets.human-diseases
  (:require
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.do-term.core :as do-term]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn detailed-disease-model [genotype]
  {:data (let [db (d/entity-db genotype)
               models (->> (d/q '[:find [?d ...]
                              :in $ ?e
                              :where
                              [?d :disease-model-annotation/modeled-by-genotype ?e]]
                            db (:db/id genotype))
                           (map (partial d/entity db)))]
           (do-term/process-disease-models models))
   :description "Curated disease associations based on experimental data"})

(def widget
  {:name generic/name-field
   :detailed_disease_model detailed-disease-model})
