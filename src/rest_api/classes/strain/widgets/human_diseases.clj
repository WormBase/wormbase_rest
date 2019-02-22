(ns rest-api.classes.strain.widgets.human-diseases
  (:require
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.do-term.core :as do-term]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn detailed-disease-model [strain]
  {:data (let [db (d/entity-db strain)
               models (->> (d/q '[:find [?d ...]
                              :in $ ?e
                              :where
                              [?eh :disease-model-annotation.modeled-by-strain/strain ?e]
                              [?d :disease-model-annotation/modeled-by-strain ?eh]]
                            db (:db/id strain))
                           (map (partial d/entity db)))]
           (do-term/process-disease-models models))
   :description "Curated disease associations based on experimental data"})

(def widget
  {:name generic/name-field
   :detailed_disease_model detailed-disease-model})
