(ns rest-api.classes.protein.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))
;
(defn estimated-molecular-weight [p]
  {:data (when-let [mwh (:protein/molecular-weight p)]
      (format "%.1f" (:protein.molecular-weight/float (first mwh))))
   :description "the estimated molecular weight of the protein"})

(defn pfam-graph [p]
  {:data nil ; work needed
   :description "The motif graph of the protein"})

(defn best-human-match [p]
  {:data nil ; requires homology data
   :description "best human BLASTP hit"})

(defn estimated-isoelectric-point [p]
  {:data nil ; This requires Bio:Java
   :description "the estimated isoelectric point of the protein"})

(defn type-field [p]
  {:data (keys p)
   :description "The type of the protein"})

(def widget
  {:name generic/name-field
 ;  :estimated_molecular_weight estimated-molecular-weight
 ;  :status generic/status status does not exist on this entity
 ;  :pfam_graph pfam-graph
 ;  :best_human_match best-human-match
 ;  :taxonomy generic/taxonomy
 ;  :description generic/description
 ;  :estimated_isoelectric_point estimated-isoelectric-point
 ;  :remarks generic/remarks
   :type type-field
   :corresponding_all generic/corresponding-all})
