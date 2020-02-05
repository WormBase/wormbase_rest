(ns rest-api.classes.structure-data.widgets.overview
  (:require
   [datomic.api :as d]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.db.main :refer [datomic-homology-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn sequence-field [s]
  {:data (when-let [protein (:structure-data/protein s)]
           (pack-obj protein))
   :description "sequence of structure"})

(defn homology-data [s]
  {:data nil
   :description "Protein homologs for this structure"})

(defn status [s]
  {:data (cond
           (contains? s :structure-data/selected)
           "Selected"

           (contains? s :structure-data/cloned)
           "cloned"

           (contains? s :structure-data/expressed)
           "Expressed")
   :description (str "current status of the Structure_data:" (:structure-data/id s) " if not Live or Valid")})


(defn protein-homology [s]
  {:data (let [db (d/entity-db s)
               hdb (d/db datomic-homology-conn)]
          (some->> (d/q '[:find [?sid ...]
                          :in $ $hdb ?sid
                          :where
                           [$hdb ?sd :structure-data/id ?sid]
                           [$hdb ?h :homology/structure ?sd]
                           [$hdb ?h :locatable/parent ?ph]
                           [$hdb ?hp :protein/id ?hpid]
                           [$ ?p :protein/id ?hpid]]
                         db hdb (:structure-data/id s))
                    (map (fn [id]
                     (let [protein (d/entity hdb id)]
                        (keys protein))))))
   :description "homology data re: this structure"})

(def widget
  {:name generic/name-field
   :sequence sequence-field
   :protein_homology protein-homology
   :status status
   :homology_data homology-data
   :remarks generic/remarks})
