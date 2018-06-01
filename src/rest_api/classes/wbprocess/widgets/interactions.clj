(ns rest-api.classes.wbprocess.widgets.interactions
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.interaction.core :as interaction]
   [rest-api.formatters.object :refer [pack-obj]]))

(defn wbprocess-direct-interactions [db wbprocess]
  (d/q '[:find ?intx ?g1h ?g2h
         :in $ % ?p
         :where
         [?p :wbprocess/interaction ?ih]
         [?ih :wbprocess.interaction/interaction ?intx]
         (interaction->x-3 ?intx ?g1h _)
         (interaction->x-3 ?intx ?g2h _)
         [(not= ?g1h ?g2h)]]
       db interaction/int-rules wbprocess))

(defn interactions
  "Produces a data structure suitable for rendering the table listing."
  [wbprocess]
  {:description "genetic and predicted interactions"
   :data (let [db (d/entity-db wbprocess)
               ints (wbprocess-direct-interactions db (:db/id wbprocess))]
           (interaction/build-interactions db ints [] interaction/arrange-interactions))})

(defn interaction-details
  "Produces a data-structure suitable for rendering a cytoscape graph."
  [wbprocess]
  {:description "addtional nearby interactions"
   :data (let [db (d/entity-db wbprocess)
               ints (wbprocess-direct-interactions db (:db/id wbprocess))]
           (interaction/build-interactions db ints [] interaction/arrange-interaction-details))})

(def widget
  {:name generic/name-field
   :interactions interactions})
