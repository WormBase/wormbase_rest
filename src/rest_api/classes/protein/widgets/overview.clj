(ns rest-api.classes.protein.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
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

(defn corresponding-all [p] ; need to add in sequence database and more work needed
  {:data (when-let [cphs (:cds.corresponding-protein/_protein p)]
           (for [cph cphs
                 :let [phs (:cds.corresponding-protein/protein p)
                       cds (first (:cds/_corresponding-protein phs))]]
             {:length_unspliced nil
              :model nil
              :gene (when-let [cds (:cds/_corresponding-protein cph)]
                      (when-let [gene (:gene/_corresponding-cds
                                        (first (:gene.corresponding-cds/_cds cds)))]
                        (pack-obj gene)))
              :keys (keys cph)
              :length_protein (:protein.peptide/length (:protein/peptide p))
              :protein (pack-obj (:cds.corresponding-protein/protein cph))
              :cds (when-let [cds (:cds/_corresponding-protein cph)]
                    {:text (pack-obj cds)
                     :evidence (obj/get-evidence cds)})
              :length_spliced nil
              :type (when-let [cds (:cds/_corresponding-protein cph)]
                      (if-let [type-field (:method/id (:locatable/method cds))]
                        (str/replace type-field #"_" " ")))}))
   :description "corresponding cds, transcripts, gene for this protein"})

(def widget
  {:name generic/name-field
   :estimated_molecular_weight estimated-molecular-weight
 ;  :status generic/status status does not exist on this entity
   :pfam_graph pfam-graph
   :best_human_match best-human-match
   :taxonomy generic/taxonomy
   :description generic/description
   :estimated_isoelectric_point estimated-isoelectric-point
   :remarks generic/remarks
   :type type-field
   :corresponding_all corresponding-all})
