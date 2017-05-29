(ns rest-api.classes.operon.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn species [o]
  {:data (when-let [s (:operon/species o)]
           (pack-obj s))
   :description "species containing the operon"})

(defn description [o]
  {:data (when-let [dh (:operon/description o)]
           (:operon.description/text dh))
   :description (str "description of the Operon" (:operon/id o))})


(def widget
  {:name generic/name-field
   :species species
   :remarks generic/remarks
   :description description})