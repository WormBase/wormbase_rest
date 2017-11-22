(ns rest-api.classes.pseudogene.widgets.genetics
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.variation.core :as variation]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn alleles [p]
  {:data (some->> (:variation.pseudogene/_pseudogene p)
                  (map :variation/_pseudogene)
                  (filter (fn [v] (contains? v :variation/snp)))
                  (map variation/process-variation))
   :description "Alleles associated with this pseudogene"})

(defn polymorphisms [p]
  {:data (some->> (:variation.pseudogene/_pseudogene p)
                  (map :variation/_pseudogene)
                  (filter (fn [v] (not (contains? v :variation/snp))))
                  (map variation/process-variation))
   :description "Polymorphisms associated with this pseudogene"})

(def widget
  {:name generic/name-field
   :alleles alleles
   :polymorphisms polymorphisms})
