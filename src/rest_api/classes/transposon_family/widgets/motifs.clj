(ns rest-api.classes.transposon-family.widgets.motifs
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn motifs [tf]
  {:data (when-let [ms (:transposon-family/associated-motif tf)]
           (for [m ms]
             (pack-obj m)))
   :description "Motifs attached to this record"})

(def widget
  {:name generic/name-field
   :motifs motifs})
