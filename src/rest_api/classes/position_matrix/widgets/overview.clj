(ns rest-api.classes.position-matrix.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn associated-feature [pm]
  {:data nil
   :description "feature associated with motif"})

(defn bound-by-gene-product [pm]
  {:data nil ; not working on main site - 500 error
   :description "gene products that bind to the motif"})

(defn description [pm]
  {:data (:position-matrix.description/text (first (:position-matrix/description pm)))
   :description (str "description of the Position_Matrix " (:position-matrix/id pm))})

(defn consensus [pm]
  {:data nil ; not producting results on main site
   :description "consensus sequence for motif"})

(defn transcription-factor [pm]
  {:data (when-let [tfs (:position-matrix/transcription-factor pm)]
           (pack-obj (:position-matrix.transcription-factor/transcription-factor (first tfs))))
   :description "Transcription factor of the feature"})

(defn associated-position-matrix [pm]
  {:data nil ; can't find any in console using (:feature/associated-with-position-matrix pm)
   :description "other matrix associated with motif"})

(defn position-data-row [base values]
  (let [value-kw (keyword "position-matrix.value" base)]
  (apply
    merge
    (conj
      (for [value values
            :let [index (format "%02d"(:ordered/index value))]]
        {index (if (not= 0.0 (value-kw value))
                 (value-kw value))})
      {:Type (str/capitalize base)}))))

(defn position-data [pm]
  {:data (when-let [values (:position-matrix/values pm)]
           (for [base ["c" "t" "g" "a"]]
             (position-data-row base values)))
   :description "data for individual positions in motif"})

(def widget
  {:name generic/name-field
   :associated_feature associated-feature
   :bound_by_gene_product bound-by-gene-product
   :description description
   :consensus consensus
   :transcription_factor transcription-factor
   :remarks generic/remarks
   :associated_positon_matrix associated-position-matrix
   :position_dat position-data})
