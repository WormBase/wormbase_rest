(ns rest-api.classes.interaction.widgets.interactions
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.interaction.core :as interaction]
   [rest-api.formatters.object :refer [pack-obj]]))

(defn interaction-direct-interactions [db interaction]
  (d/q '[:find ?intx ?g1h ?g2h
         :in $ % ?intx
         :where
         (interaction-> ?intx ?g1h _)
         (interaction-> ?intx ?g2h _)
         [(not= ?g1h ?g2h)]]
       db interaction/int-rules interaction))

(defn interactions
  "Produces a data structure suitable for rendering the table listing."
  [interaction]
  {:description "genetic and predicted interactions"
   :data (let [db (d/entity-db interaction)]
           (interaction/build-interactions db
                                           (partial interaction-direct-interactions db (:db/id interaction))
                                           nil
                                           :graph-only-mode? false))})

(defn interaction-details
  "Produces a data-structure suitable for rendering a cytoscape graph."
  [interaction]
  {:description "addtional nearby interactions"
   :data (let [db (d/entity-db interaction)]
           (interaction/build-interactions db
                                           (partial interaction-direct-interactions db (:db/id interaction))
                                           nil
                                           :graph-only-mode? true))})

(def widget
  {:name generic/name-field
   :interactions interactions})
