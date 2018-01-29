(ns rest-api.classes.anatomy-term.widgets.expression-markers
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn markers [a]
  {:data (some->> (:expr-pattern.anatomy-term/_anatomy-term a)
                  (map (fn [h]
                         (let [e (:expr-pattern/_anatomy-term h)];
                         {:expression_pattern (pack-obj e)
                          :gene (some->> (:expr-pattern/gene e)
                                         (map :expr-pattern.gene/gene)
                                         (map pack-obj))
                          :description (:expr-pattern/pattern e)
                          :certainty (first (:qualifier/certain h))}))))
   :description (str "Expression markers for the anatomy term: " (:anatomy-term/id a))})

(def widget
  {:name generic/name-field
   :markers markers})
