(ns rest-api.classes.strain.widgets.natural-isolates
  (:require
    [datomic.api :as d]
    [rest-api.db.main :refer [datomic-conn]]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-fns]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn natural-isolates [s]
  {:data (let [db (d/db datomic-conn)]
           (->> (d/q '[:find [?s ...]
                        :in $
                        :where
                        [?s :strain/id]
                        [?s :strain/wild-isolate]]
                      db)
                (map (fn [id]
                       (let [obj (d/entity db id)]
                         {:substrate (:strain/substrate obj)
                          :longitude (when-let [longitude (:strain.geolocation/longitude
                                                            (:strain/geolocation obj))]
                                      (str (generic-fns/round longitude :precision 2)))
                          :place (:strain/place obj)
                          :species (:species/id (:strain/species obj))
                          :isolated-by (some->> (:strain/isolated-by obj)
                                                (first)
                                                (pack-obj))
                          :strain (pack-obj obj)
                          :latitude (when-let [latitude (:strain.geolocation/latitude
                                                          (:strain/geolocation obj))]
                                      (str (generic-fns/round latitude :precision 2)))
                          :landscape (when-let [landscape-kw (:strain/landscape obj)]
                                       (let [landscape-name (name landscape-kw)]
                                          (obj/humanize-ident landscape-name)))})))))
   :description "a list of wild isolates of strains contained in WormBase"})

(def widget
  {:name generic/name-field
   :naturlal_isolates natural-isolates})
