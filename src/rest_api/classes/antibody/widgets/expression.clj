(ns rest-api.classes.antibody.widgets.expression
  (:require
    [clojure.string :as str]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.expr-pattern.core :as expr-pattern]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.classes.generic-fields :as generic]))

(defn expression-patterns [a]
  {:data (some->> (:expr-pattern/_antibody-info a)
                  (map (fn [ep]
                           (expr-pattern/pack ep nil))))
   :description (str "expression patterns associated with the antibody: " (:antibody/id a))})

(def widget
  {:name generic/name-field
   :expression_patterns expression-patterns})
