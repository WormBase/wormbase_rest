(ns rest-api.classes.homology-group.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn proteins [h]
  {:data (when-let [proteins (:homology-group/protein h)]
           (for [protein proteins]
             {:protein (pack-obj protein)
              :species (when-let [species (:protein/species protein)]
                         (pack-obj species))
              :description (:protein/description protein)}))
   :description "proteins related to this homology_group"})

(defn title [h]
  {:data nil
   :description "title for this homology group"})

(defn type-field [h]
  {:data (not-empty
           (cond
             (contains? h :homology-group/cog-code)
             {:homology_group "COG"
              :evidence (obj/get-evidence (:homology-group/cog-code h))} ; not sure if this is how I should get evidence

             (contains? h :homology-group/eggnog-code)
             {:homology_group "eggNOG"
              :evidence (obj/get-evidence (:homology-group/eggnog-code h))}))
   :description "type of homology group"})

(defn gene-ontology-terms [h]
  {:data nil ; This requires  :homology-group/go-term  but there are no instances in the db of it existing
   :description "gene ontology terms associated to this homology group"})

(def widget
  {:name generic/name-field
   :proteins proteins
   :remarks generic/remarks
   :title title
   :type type-field
   :gene_ontology_terms gene-ontology-terms})
