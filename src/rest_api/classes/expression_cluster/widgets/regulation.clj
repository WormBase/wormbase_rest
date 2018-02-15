(ns rest-api.classes.expression-cluster.widgets.regulation
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn regulated-by-molecule [ec]
  {:data (when-let [molecules (:expression-cluster/regulated-by-molecule ec)]
           (map pack-obj molecules))
   :description "Molecule regulating this expression cluster"})

(defn regulated-by-gene [ec]
  {:data (when-let [genes (:expression-cluster/regulated-by-gene ec)]
           (map pack-obj genes))
   :description "Gene regulating this expression cluster"})

(defn regulated-by-treatment [ec]
  {:data (first (:expression-cluster/regulated-by-treatment ec))
   :description "Treatment regulating this expression cluster"})

(def widget
  {:name generic/name-field
   :regulated_by_molecule regulated-by-molecule
   :regulated_by_gene regulated-by-gene
   :regulated_by_treatment regulated-by-treatment})
