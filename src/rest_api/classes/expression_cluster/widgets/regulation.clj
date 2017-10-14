(ns rest-api.classes.expression-cluster.widgets.regulation
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn regulation-by-molecule [ec]
  {:data (when-let [molecules (:expression-cluster/regulated-by-molecule ec)]
           (map pack-obj molecules))
   :description "Molecule regulating this expression cluster"})

(defn regulation-by-gene [ec]
  {:data (when-let [genes (:expression-cluster/regulated-by-gene ec)]
           (map pack-obj genes))
   :description "Gene regulating this expression cluster"})

(defn regulation-by-treatment [ec]
  {:data (first (:expression-cluster/regulated-by-treatment ec))
   :description "Treatment regulating this expression cluster"})

(def widget
  {:name generic/name-field
   :regulation_by_molecule regulation-by-molecule
   :regulation_by_gene regulation-by-gene
   :regulation_by_treatment regulation-by-treatment})
