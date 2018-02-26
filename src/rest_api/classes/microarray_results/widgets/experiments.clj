(ns rest-api.classes.microarray-results.widgets.experiments
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- range-field [m]
  {:data {:Max (let [h (:microarray-results/max m)]
                 {:val (:microarray-results.max/value h)
                  :experiment (:microarray-experiment/id (:microarray-results.max/experiment h))})
          :Min (let [h (:microarray-results/min m)]
                 {:val (:microarray-results.min/value h)
                  :experiment (:microarray-experiment/id (:microarray-results.min/experiment h))})}
   :description "The range of the microarray results"})

(defn results [m]
  {:data (some->> (:microarray-results/results m)
                  (map :microarray-results.results/microarray-experiment)
                  (map (fn [e]
                         {:experiment (:microarray-experiment/id e)
                          :temp (first
                                  (:condition/temperature
                                    (:microarray-experiment/microarray-sample e)))
                          :references (some->> (:microarray-experiment/reference e)
                                               (map pack-obj))
                          :life_stage (when-let [ls (:condition/life-stage
                                                      (:microarray-experiment/microarray-sample e))]
                                        (pack-obj (first ls)))
                          :clusters (some->> (:expression-cluster/_microarray-experiment e)
                                             (map pack-obj))})))
   :description "The corresponding cds"})

(defn microarray [m]
  {:data (some->> (:microarray-results/microarray m)
                  (map (fn [chip]
                         {:info (let [info (first (:microarray/chip-info chip))]
                                  (if-let [remark (map :microarray.remark/text
                                                       (:microarray/remark chip))]
                                    (flatten
                                      [info
                                       "<b>Remarks:</b>"
                                       remark])
                                    info))
                          :papers (some->> (:microarray/reference chip)
                                           (map pack-obj))
                          :experiments (count (:microarray-experiment/_microarray chip))
                          :type (:microarray/chip-type chip)})))
   :description "Details about the microarray"})

(def widget
  {:name generic/name-field
   :range range-field
   :results results
   :miroarray microarray})
