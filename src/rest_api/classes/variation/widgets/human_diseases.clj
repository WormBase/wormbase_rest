(ns rest-api.classes.variation.widgets.human-diseases
  (:require
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.do-term.core :as do-term]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn detailed-disease-model [variation]
  {:data (let [db (d/entity-db variation)
               models (->> (d/q '[:find [?d ...]
                              :in $ ?e
                              :where
                              [?eh :disease-model-annotation.modeled-by-variation/variation ?e]
                              [?d :disease-model-annotation/modeled-by-variation ?eh]]
                            db (:db/id variation))
                           (map (partial d/entity db)))]
           (do-term/process-disease-models models))
   :description "Detailed disease model"})

(def widget
  {:name generic/name-field
   :detailed_disease_model detailed-disease-model})
