(ns rest-api.classes.picture.core
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn pack-image [picture]
  (let [prefix (if (re-find #"<Journal_URL>" (or (:picture/acknowledgement-template picture) ""))
                 (:paper/id (first (:picture/reference picture)))
                 (:person/id (first (:picture/contact picture))))]
    (if-let [[_ picture-name format-name] (re-matches #"(.+)\.(.+)" (or (:picture/name picture) ""))]
      (-> picture
          (pack-obj)
          (assoc :thumbnail
                 {:format (or format-name "")
                  :name (str prefix "/" (or picture-name (:picture/name picture)))
                  :class "/img-static/pictures"}

                 :description
                 (if-let [expr-patterns (seq (:picture/expr-pattern picture))]
                   (->> (map :expr-pattern/id expr-patterns)
                        (str/join ", ")
                        (str "curated pictures for "))))))))

(defn external-sources [picture]
  (not-empty
    (pace-utils/vmap
      :template
      (:picture/acknowledgement-template picture)

      :template_items
      (not-empty
	(pace-utils/vmap
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
	     :text (:database/name d)}))))))

