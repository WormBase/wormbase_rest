(ns rest-api.classes.transposon-family.widgets.var-motifs
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn motifs [tf]
  {:data (when-let [ms (:transposon-family/associated-motif tf)]
           (map pack-obj ms))
   :description "Motifs attached to this record"})

(defn variations [tf]
  {:data (when-let [vs (:variation/_transposon-insertion tf)]
           (for [v vs]
             {:id (pack-obj v)
              :species (when-let [s (:variation/species v)]
                         (pack-obj s))
              :gene (when-let [ghs (:variation/gene v)]
                      (for [g ghs
                            :let [gene (:variation.gene/gene g)]]
                        (pack-obj gene)))}))
   :description "Variations attached to this record"})

(def widget
  {:name generic/name-field
   :variations variations
   :motifs motifs})
