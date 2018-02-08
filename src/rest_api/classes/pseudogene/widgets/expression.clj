(ns rest-api.classes.pseudogene.widgets.expression
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn microarray-results [p]
  {:data (some->> (:microarray-results.pseudogene/_pseudogene p)
                  (map :microarray-results/_pseudogene)
                  (map pack-obj)
                  (sort-by :label))
   :description "Microarray results"})

(def widget
  {:name generic/name-field
   :microarray_results microarray-results})
