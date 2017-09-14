(ns rest-api.classes.laboratory.widgets.gene-classes
  (:require
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic-fields :as generic]))

(defn former-gene-classes [laboratory]
  (let [db (d/entity-db laboratory)
        data (->> (d/q '[:find ?gene-class ?description
                         :in $ ?laboratory
                         :where 
                         [?laboratoryent :gene-class.former-designating-laboratory/laboratory ?laboratory]
                         [?gc :gene-class/former-designating-laboratory ?laboratoryent]
                         [?gc :gene-class/id ?gene-class]
                         [?gc :gene-class/description ?description]]
                       db (:db/id laboratory))
                  (map (fn [oid]
                         (let [gene-class (get oid 0)
                               description (get oid 1)]
                           (pace-utils/vmap
                             :description description
                             :former_gene_class
                             (pace-utils/vmap
                               :taxonomy "all"
                               :class "gene_class"
                               :label gene-class
                               :id gene-class)))))
                  (seq))]
    {:data data
     :description "former gene classes assigned to laboratory"}))

(defn gene-classes [laboratory]
  (let [db (d/entity-db laboratory)
        data (->> (d/q '[:find ?gene-class ?description
                         :in $ ?laboratory
                         :where 
                         [?gc :gene-class/designating-laboratory ?laboratory]
                         [?gc :gene-class/id ?gene-class]
                         [?gc :gene-class/description ?description]]
                       db (:db/id laboratory))
                  (map (fn [oid]
                         (let [gene-class (get oid 0)
                               description (get oid 1)]
                           (pace-utils/vmap
                             :description description
                             :gene_class
                             (pace-utils/vmap
                               :taxonomy "all"
                               :class "gene_class"
                               :label gene-class
                               :id gene-class)))))
                  (seq))]
    {:data data
     :description "gene classes assigned to laboratory"}))

(def widget
  {:name generic/name-field
   :gene_classes gene-classes
   :former_gene_classes former-gene-classes})
