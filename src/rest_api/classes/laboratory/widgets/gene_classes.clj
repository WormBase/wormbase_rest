(ns rest-api.classes.laboratory.widgets.gene-classes
  (:require
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic-fields :as generic]))

(defn former-gene-classes [laboratory]
  (let [db (d/entity-db laboratory)
        data (->> (d/q '[:find [?gc ...]
                         :in $ ?laboratory
                         :where 
                         [?laboratoryent :gene-class.former-designating-laboratory/laboratory ?laboratory]
                         [?gc :gene-class/former-designating-laboratory ?laboratoryent]]
                       db (:db/id laboratory))
                  (map (fn [gc]
                         (let [gene-class (d/entity db gc)]
                           {:description (:gene-class/description gene-class)
                            :former_gene_class (pack-obj "gene-class" gene-class)})))
                  (seq))]
    {:data data
     :description "former gene classes assigned to laboratory"}))

(defn gene-classes [laboratory]
  (let [db (d/entity-db laboratory)
        data (->> (d/q '[:find [?gc ...]
                         :in $ ?laboratory
                         :where 
                         [?gc :gene-class/designating-laboratory ?laboratory]]
                       db (:db/id laboratory))
                  (map (fn [gc]
                         (let [gene-class (d/entity db gc)]
                           {:description (:gene-class/description gene-class)
                            :gene_class (pack-obj "gene-class" gene-class)})))
                  (seq))]
    {:data data
     :description "gene classes assigned to laboratory"}))

(def widget
  {:name generic/name-field
   :gene_classes gene-classes
   :former_gene_classes former-gene-classes})
