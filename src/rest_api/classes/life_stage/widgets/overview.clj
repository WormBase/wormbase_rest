(ns rest-api.classes.life-stage.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn preceded-by-life-stage [ls]
  {:data (keys ls)
   :dbid (:db/id ls)
   :description "preceded by life stage"})

(defn cells [ls]
  {:data nil
   :description "cells at this lifestage"})

(defn cell-group [ls]
  {:data nil
   :description "The prominent cell group for this life stage"})

(defn definition [ls]
  {:data (first (:life-stage/definition ls))
   :description "brief definition  of the life stage"})

(defn contained-in-life-stage [ls]
  {:data (when-let [cis (:life-stage/contained-in ls)]
           (for [ci cis]
             (pack-obj ci)
             )
           )
   :description "contained in life stage"})

(defn remarks [ls]
  {:data nil
   :description "curatorial remarks for the Life_stage"})

(defn substages [ls]
  {:data nil
   :description "life substage"})

(defn followed-by-life-stage [ls]
  {:data nil
   :description "next life stage after this"})

(defn other-names [ls]
  {:data nil
   :description (str "other names that have been used to refer to " (:life-stage/id ls))})

(def widget
  {:name generic/name-field
   :preceded_by_life_stage preceded-by-life-stage
   :cells cells
   :cell_group cell-group
   :definition definition
   :contained_in_life_stage contained-in-life-stage
   :remarks remarks
   :substages substages
   :followed_by_life_stage followed-by-life-stage
   :other_names other-names})
