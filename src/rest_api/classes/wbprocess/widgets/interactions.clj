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
         (interaction-> ?intx ?g1h ?it1 _)
         (interaction-> ?intx ?g2h ?it2 _)
         (not-join [?g1h ?g2h ?it1 ?it2]
                   [(= ?g1h ?g2h)]
                   [(= ?it1 ?it2)])]
       db interaction/int-rules wbprocess))

(defn interactions
  "Produces a data structure suitable for rendering the table listing."
  [wbprocess]
  {:description "genetic and predicted interactions"
   :data (let [db (d/entity-db wbprocess)]
           (interaction/build-interactions db (wbprocess-direct-interactions db (:db/id wbprocess))))})

(defn interaction-details
  "Produces a data-structure suitable for rendering a cytoscape graph."
  [wbprocess]
  {:description "addtional nearby interactions"
   :data (let [db (d/entity-db wbprocess)]
           (interaction/build-interactions-graph db (wbprocess-direct-interactions db (:db/id wbprocess)) nil))})

(def widget
  {:name generic/name-field
   :interactions interactions})
