(ns rest-api.classes.life-stage.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn preceded-by-life-stage [ls]
  {:data (some->> (:life-stage/preceded-by ls)
                  (map pack-obj)
                  (sort-by :label))
   :description "preceded by life stage"})

(defn cells [ls]
  {:data (some->> (:life-stage/anatomy-term ls)
                  (map pack-obj)
                  (sort-by :label))
   :description "cells at this lifestage"})

;(defn cell-group [ls]
;  {:data nil ; this requires the cell group field and is commented out in the ace schema file. WBls:0000032 is an example of it populated.
;   :description "The prominent cell group for this life stage"})

(defn definition [ls]
  {:data (->> (:life-stage/definition ls)
              (:life-stage.definition/text))
   :description "brief definition  of the life stage"})

(defn contained-in-life-stage [ls]
  {:data (some->> (:life-stage/contained-in ls)
                  (map pack-obj)
                  (sort-by :label))
   :description "contained in life stage"})

(defn substages [ls]
  {:data (some->> (:life-stage/_contained-in ls)
                  (map pack-obj)
                  (sort-by :label))
   :description "life substage"})

(defn followed-by-life-stage [ls]
  {:data (some->> (:life-stage/_preceded-by ls)
                  (map pack-obj)
                  (sort-by :label))
   :description "next life stage after this"})

(def widget
  {:name generic/name-field
   :preceded_by_life_stage preceded-by-life-stage
   :cells cells
   ;:cell_group cell-group
   :definition definition
   :contained_in_life_stage contained-in-life-stage
   :remarks generic/remarks
   :substages substages
   :followed_by_life_stage followed-by-life-stage
   :other_names generic/other-names})
