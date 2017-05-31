(ns rest-api.classes.expr-pattern.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
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
           (group-by :paper_id
           (for [picture pictures
                 :let [n (:picture/name picture)
                       paper  (first (:picture/reference picture))]]
	      {:source (when-let [paper (:picture/reference picture)]
			 (pack-obj (first paper)))
	       :draw (when n
		       (let [[basename extension] (split-last "." n)]
			 {:format extension
			  :name (str/join "/" [(:paper/id paper) basename])
			  :class "/img-static/pictures"}))
	       :external_source {:template (:picture/acknowledgement-template picture)
                                 :template-items (pace-utils/vmap
                                                   :Article_URL
                                                   (when-let [ah (:picture/article-url picture)]
                                                     (let [d (:picture.article-url/database ah)]
                                                       {:db (:database/id d)
                                                        :text (:database/name d)}))

                                                  :Journal_URL
                                                  (when-let [d (:picture/journal-url picture)]
                                                      {:db (:database/id d)
                                                       :text (:database/name d)})

                                                  :Publisher_URL
                                                  (when-let [d (:picture/publisher-url picture)]
                                                      {:db (:database/id d)
                                                       :text (:database/name d)}))}
              :paper_id (:paper/id paper)
              :id (:picture/id picture)})))
   :description "Curated images of the expression pattern"})

(defn description [ep]
  {:data (when-let [patterns (:expr-pattern/pattern ep)]
           (str/join "<br />" patterns))
   :description (str "description of the Expr_pattern " (:expr-pattern/id ep))})

(defn ep-movies [ep]
  {:data (:expr-pattern/movieurl ep) ; need to wait for Sybils changes to know what it should be like
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

(defn subcellular-location [ep]
  {:data (:expr-pattern/subcellular-localization ep)
   :description "Subcellular locations of this expression pattern"})

(defn expression-image [ep]
  {:data (str "/img-static/virtualworm/Gene_Expr_Renders/" (:expr-pattern/id ep) ".jpg")
   :description "Image of the expression pattern"})

(defn is-bs-strain [ep]
  {:data (when-let [labs (:expr-pattern/laboratory ep)] ; I am not sure how to replicate logic from perl - Expr106 has it
           (let [lab (first labs)]
             (keys lab)))
   :description "Whether this is expression pattern for a BC strain"})

(def widget
  {:name generic/name-field
   :curated_images curated-images
   :description description
   :ep_movies ep-movies
   :database database
   :remarks generic/remarks
   :historical_gene generic/historical-gene
   :subcellular_location subcellular-location
   :expression_image expression-image
   :is_bs_strain is-bs-strain})
