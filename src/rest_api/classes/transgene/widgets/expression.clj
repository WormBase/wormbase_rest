(ns rest-api.classes.transgene.widgets.expression
  (:require
    [clojure.string :as str]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.expr-pattern.core :as expr-pattern]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.classes.generic-fields :as generic]))

(defn marker-for [t]
  {:data (some->> (:transgene/marker-for t)
                  (first)
                  (:transgene.marker-for/text))
   :description "string decribing what the transgene is a marker for"})

(defn expression-patterns [t]
  {:data (some->> (:expr-pattern/_transgene t)
                  (map (fn [ep]
                           (expr-pattern/pack ep nil))))
   :description (str "expression patterns associated with the transgene: " (:transgene/id t))})

(def widget
  {:name generic/name-field
   :marker_for marker-for
   :expression_patterns expression-patterns})
