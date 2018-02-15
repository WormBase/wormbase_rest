(ns rest-api.classes.feature.widgets.evidence
  (:require
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn defined-by [f]
  {:data (not-empty
           (flatten
             (vals
               (pace-utils/vmap
                 :Person
                 (some->> (:feature/defined-by-person f)
                          (map #(pace-utils/vmap
                                  :object (pack-obj %)
                                  :label "Person")))

                 :Analysis
                 (some->> (:feature/defined-by-analysis f)
                          (map :feature.defined-by-analysis/analysis)
                          (map #(pace-utils/vmap
                                  :object (pack-obj %)
                                  :label "Analysis")))

                 :Paper
                 (some->> (:feature/defined-by-paper f)
                          (map :feature.defined-by-paper/paper)
                          (map #(pace-utils/vmap
                                  :object (pack-obj %)
                                  :label "Paper")))

                 :Sequence
                 (some->> (:feature/defined-by-sequence f)
                          (map :feature.defined-by-sequence/sequence)
                          (map #(pace-utils/vmap
                                  :object (pack-obj %)
                                  :label "Sequence")))))))

   :description "how the sequence feature was defined"})

(defn remarks [f]
  {:data (some->> (:feature/remark f)
                  (map (fn [h]
                          {:text (:feature.remark/text h)
                           :evidence (obj/get-evidence h)})))
   :description "curatorial remarks for the Feature"})

(def widget
  {:name generic/name-field
   :defined_by defined-by
   :remarks remarks})
