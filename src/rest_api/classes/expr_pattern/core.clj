(ns rest-api.classes.expr-pattern.core
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn pack [ep ep-holder]
  (let [h (first (:expr-pattern/gene ep))
        gene (:expr-pattern.gene/gene h)]
    {:certainty (generic-functions/certainty ep-holder)
     :expression_pattern (pack-obj ep)
     :reference (some->> (:expr-pattern/reference ep)
                         (map :expr-pattern.reference/paper)
                         (map pack-obj))
     :gene (some->> (:expr-pattern/gene ep)
                    (map :expr-pattern.gene/gene)
                    (map pack-obj))
     :author (:author/id (first (:expr-pattern/author ep)))
     :description (or
                    (some->> (:expr-pattern/pattern ep)
                             (str/join "<br />"))
                    (or
                      (some->> (:expr-pattern/subcellular-localization ep)
                               (sort)
                               (str/join "<br />"))
                      (some->> (:expr-pattern/remark ep)
                               (map :expr-pattern.remark/text)
                               (sort)
                               (str/join "<br />"))))}))
