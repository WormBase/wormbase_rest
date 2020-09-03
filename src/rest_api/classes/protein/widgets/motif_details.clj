(ns rest-api.classes.protein.widgets.motif-details
  (:require
   [datomic.api :as d]
   [clojure.string :as str]
   [rest-api.classes.protein.core :as protein-core]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :refer [xform-species-name]]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.db.sequence :refer [species-assemblies]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn motif-details [p]
 {:data (protein-core/get-motif-details p)
  :description "The motif details of the protein"})

(defn schematic-parameters [p]
  {:data (let [db (d/entity-db p)
               cds-name (some->> (d/q '[:find ?cds .
                                        :in $ ?p
                                        :where
                                        [?ph :cds.corresponding-protein/protein ?p]
                                        [?cds :cds/corresponding-protein ?ph]]
                                      db
                                      (:db/id p))
                            (d/entity db)
                            (:cds/id))
               peptide-length (some->> (:protein/peptide p)
                                       (:protein.peptide/length))
               g-species (->> (:protein/species p)
                              (:species/other-name)
                              (first)
                              (xform-species-name))
               assembly (->>
                         (get-in species-assemblies [g-species "assemblies"])
                         (filter #(= (get % "is_canonical") true))
                         (first))]

           (if (and cds-name peptide-length)
             {:location (format "%s:1..%s" cds-name peptide-length)
              :reference_id cds-name
              :species g-species
              :bioproject (get assembly "bioproject")}))
   :description "jbrowse location in protein schematics"})

(def widget
  {:name generic/name-field
   :motif_details motif-details
   :schematic_parameters schematic-parameters})
