(ns rest-api.classes.transposon.widgets.associations
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn sequence-field [t]
  {:data (when-let [chs (:transposon/corresponding-cds t)] t
           (for [ch chs
                 :let [cds (:transposon.corresponding-cds/cds ch)]]
             (pack-obj cds)))
   :description "Sequences associated with this transposon"})

(defn gene [t]
  {:data (when-let [gths (:gene.corresponding-transposon/_transposon t)]
           (for [gth gths
                 :let [gene (:gene/_corresponding-transposon gth)]]
             (pack-obj gene)))
   :description "Gene(s) associated with this transposon"})

(def widget
  {:name generic/name-field
   :gene gene
   :sequence sequence-field})
