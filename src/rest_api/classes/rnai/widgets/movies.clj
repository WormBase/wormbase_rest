(ns rest-api.classes.rnai.widgets.movies
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn movies [r]
  {:data (some->> (:movie/_rnai r)
                  (map (fn [m]
                         {:name (:movie/id m)
                          :file (:movie.db-info/accession
                                  (first
                                    (:movie/db-info m)))
                          :class (:movie.db-info/accession
                                   (first
                                     (:movie/db-info m)))
                          :label (:movie/id m)}))
                  (sort-by :label))
   :description "movies documenting effect of rnai"})

(def widget
  {:name generic/name-field
   :movies movies})
