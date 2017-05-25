(ns rest-api.classes.life-stage.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn preceded-by-life-stage [ls]
  {:data (when-let [pbs (:life-stage/preceded-by ls)]
           (for [pb pbs]
             (pack-obj pb)))
   :dbid (:db/id ls)
   :description "preceded by life stage"})

(defn cells [ls]
  {:data (when-let [terms (:life-stage/anatomy-term ls)]
           (for [term terms]
             (pack-obj term)))
   :description "cells at this lifestage"})

(defn cell-group [ls]
  {:data nil ; this requires the cell group field and is commented out in the ace schema file. WBls:0000032 is an example of it populated.
   :description "The prominent cell group for this life stage"})

(defn definition [ls]
  {:data (first (:life-stage/definition ls))
   :description "brief definition  of the life stage"})

(defn contained-in-life-stage [ls]
  {:data (when-let [cis (:life-stage/contained-in ls)]
           (for [ci cis]
             (pack-obj ci)))
   :description "contained in life stage"})

(defn remarks [ls]
  {:data (if-let [rs (:life-stage/remark ls)]
           (for [r rs]
             {:text (:life-stage.remark/text r)
              :evidence nil}))
   :description "curatorial remarks for the Life_stage"})

(defn substages [ls]
  {:data (when-let [ss (:life-stage/_contained-in ls)]
           (for [s ss]
             (pack-obj s)))
   :description "life substage"})

(defn followed-by-life-stage [ls]
  {:data  (when-let [pbs (:life-stage/_preceded-by ls)]
           (for [pb pbs]
             (pack-obj pb)))
   :description "next life stage after this"})

(defn other-names [ls]
  {:data (when-let [ons (:life-stage/other-name ls)]
           ons)
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
