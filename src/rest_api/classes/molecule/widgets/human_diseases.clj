(ns rest-api.classes.molecule.widgets.human-diseases
  (:require
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.do-term.core :as do-term]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn detailed-disease-model [molecule]
  {:data (let [db (d/entity-db molecule)
               models (->> (d/q '[:find [?d ...]
                              :in $ ?e
                              :where
                                  (or [?d :disease-model-annotation/inducing-chemical ?e]
                                      [?d :disease-model-annotation/modifier-molecule ?e])]
                            db (:db/id molecule))
                           (map (partial d/entity db)))]
           (do-term/process-disease-models models))
   :description "Curated disease associations based on experimental data"})

(def widget
  {:name generic/name-field
   :detailed_disease_model detailed-disease-model
   })
