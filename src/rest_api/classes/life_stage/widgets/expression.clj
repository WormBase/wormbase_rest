(ns rest-api.classes.life-stage.widgets.expression
  (:require
    [clojure.string :as str]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.expr-pattern.core :as expr-pattern]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.classes.generic-fields :as generic]))

(defn expression-patterns [l]
  {:data (some->> (:expr-pattern.life-stage/_life-stage l)
                  (map (fn [h]
                         (let [ep (:expr-pattern/_life-stage h)]
                           (expr-pattern/pack ep h)))))
   :description (str "expression patterns associated with the Life_stage: " (:life-stage/id l))})

(def widget
  {:name generic/name-field
   :expression_patterns expression-patterns})
