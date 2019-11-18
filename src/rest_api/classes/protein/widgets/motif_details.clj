(ns rest-api.classes.protein.widgets.motif-details
  (:require
   [datomic.api :as d]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn motif-details [p]
  {:data (let [db-homology (d/db datomic-homology-conn)
               db (d/db datomic-conn)
               plength (->> p :protein/peptide :protein.peptide/length)]
          (some->> (d/q '[:find [?h ...]
                          :in $hdb ?pid
                          :where
                           [$hdb ?e :protein/id ?pid]
                           [$hdb ?h :homology/protein ?e]]
                          db-homology
                          (:protein/id p))
                   (map (fn [id]
                         (let [obj (d/entity db-homology id)]
                          {:obj (keys obj)
                           :parent (keys (:locatable/parent obj))
                           :protein (keys (:homology/protein obj))})))))

   :d (:db/id p)
   :description "The motif details of the protein"})

(def widget
  {:name generic/name-field
   :motif_details motif-details})
