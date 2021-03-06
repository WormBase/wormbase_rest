(ns rest-api.classes.expr-pattern.widgets.overview
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.picture.core :as picture-fns]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- index-of
  [^String s c]
  (.indexOf s c))

(defn- last-index-of
  [^String s c]
  (.lastIndexOf s c))

(defn- split-at-index-fn
  [f c s]
  (let [i (f s c)]
    (when-not (neg? i)
      [(.substring s 0 i) (.substring s (inc i))])))

(def split-first
  (partial split-at-index-fn index-of))

(def split-last
  (partial split-at-index-fn last-index-of))

(defn curated-images [ep]
  {:data (when-let [pictures (:picture/_expr-pattern ep)]
           (group-by :group_id
           (for [picture pictures
                 :let [n (:picture/name picture)
                       paper (first (:picture/reference picture))
                       id (or (:person/id (first (:picture/contact picture)))
                                     (:paper/id paper))]]
	      {:source (or (some->> (:picture/contact picture)
                                (first)
                                (pack-obj))
                           (when (some? paper)
                             (pack-obj paper)))
	       :draw (when n
		       (let [[basename extension] (split-last "." n)]
			 {:format extension
			  :name (str/join "/" [id basename])
			  :class "/img-static/pictures"}))
               :external_source (picture-fns/external-sources picture)
              :group_id id
              :id (:picture/id picture)})))
   :description "Curated images of the expression pattern"})

(defn description [ep]
  {:data (when-let [patterns (:expr-pattern/pattern ep)]
           (str/join "<br />" patterns))
   :description (str "description of the Expr_pattern " (:expr-pattern/id ep))})

(defn ep-movies [ep]
  {:data (->> (:movie/_expr-pattern ep)
              (map pack-obj)
              (seq))
   :description "Movies showcasing this expression pattern"})

(defn database [ep]
  {:data (when-let [dbhs (:expr-pattern/db-info ep)]
           (for [dbh dbhs :let [database (:database/name
                                           (:expr-pattern.db-info/database dbh))
                                geneid (:expr-pattern.db-info/accession dbh)]]
             {:id geneid
              :label database
              :class database}))
   :description "Database for this expression pattern"})

(defn subcellular-locations [ep]
  {:data (:expr-pattern/subcellular-localization ep)
   :description "Subcellular locations of this expression pattern"})

(defn expression-image [ep]
  {:data (str "/img-static/virtualworm/Expr_Object_Renders/" (:expr-pattern/id ep) ".jpg")
   :description "Image of the expression pattern"})

(defn is-bs-strain [ep] ; this was taken from the perl code; there are no examples in database of this bine true
  {:data (when-let [labs (:expr-pattern/laboratory ep)]
           (let [lab (first labs)]
             (when (or (:laboratory/id lab) "BC")
                     (:laboratory/id lab) "VC"))
                 "true")
   :description "Whether this is expression pattern for a BC strain"})

(def widget
  {:name generic/name-field
   :curated_images curated-images
   :description description
   :ep_movies ep-movies
   :database database
   :remarks generic/remarks
   :historical_gene generic/historical-gene
   :subcellular_locations subcellular-locations
   :expression_image expression-image
   :is_bs_strain is-bs-strain})
