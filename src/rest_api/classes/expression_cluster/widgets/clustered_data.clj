(ns rest-api.classes.expression-cluster.widgets.clustered-data
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn microarray [ec]
  {:data (when-let [mrhs (:expression-cluster/microarray-results ec)]
           (for [mrh mrhs
                 :let [mr (:expression-cluster.microarray-results/microarray-results mrh)]]
             {:minimum (:microarray-results.min/value
                         (:microarray-results/min mr))
              :experiment (when-let [result (:microarray-results.results/microarray-experiment
                                              (first (:microarray-results/results mr)))]
                              (pack-obj result))
              :maximum (:microarray-results.max/value
                         (:microarray-results/max mr))
              :microarray (pack-obj mr)}))
   :description "microarray results from expression cluster"})

(defn sage-tags [ec] ; non in the database
  {:data (when-let [sths (:expression-cluster/sage-tag ec)]
           (for [sth sths
                 :let [sage-tag (:expression-cluster.sage-tag/sage-tag sth)]]
              (pack-obj sage-tag)))
   :description "Sage tags associated with this expression_cluster"})

(def widget
  {:name generic/name-field
   :microarray microarray
   :sage_tags sage-tags})
