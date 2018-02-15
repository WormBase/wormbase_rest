(ns rest-api.classes.expr-pattern.core
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn pack [ep]
  (let [h (first (:expr-pattern/gene ep))
	gene (:expr-pattern.gene/gene h)]
    {:certainty (generic-functions/certainty h)
     :expression_pattern {:taxonomy "all"
			  :class "expr_pattern"
                          :label (str "Expression pattern for "
                                      (if-let [n (or (:gene/public-name gene)
                                                     (:gene/id gene))]
                                        n
                                        ""))
			  :id (:expr-pattern/id ep)}
     :reference (some->> (:expr-pattern/reference ep)
			 (map :expr-pattern.reference/paper)
			 (map :paper/id)
			 (first))
     :gene (if-not (empty? gene) (pack-obj gene))
     :author (:author/id (last (:expr-pattern/author ep)))
     :description (first (:expr-pattern/pattern ep))}))
