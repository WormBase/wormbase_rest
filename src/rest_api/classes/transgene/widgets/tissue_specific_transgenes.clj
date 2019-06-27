(ns rest-api.classes.transgene.widgets.tissue-specific-transgenes
  (:require
    [datomic.api :as d]
    [rest-api.db.main :refer [datomic-conn]]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-fns]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn tissue-specific-transgenes [s]
  {:data (let [db (d/db datomic-conn)]
           (->> (d/q '[:find [?s ...]
                        :in $
                        :where
                        [?s :transgene/marker-for]
                        [?s :transgene/id]]
                      db)
                (map (fn [id]
                       (let [obj (d/entity db id)]
                         {:transgene (pack-obj obj)
                          :marker_for (some->> (:transgene/marker-for obj)
                                               (map :transgene.marker-for/text)
                                               (map pack-obj)
                                               (first))
                          :reference (some->> (:transgene/reference obj)
                                              (map :transgene.reference/paper)
                                              (map pack-obj)
                                              ;(map keys)
                                              (remove nil?)
                                              (first))
                          :summary (:transgene.summary/text
                                     (:transgene/summary obj))})))))
   :description "tissue-specific genes"})

(def widget
  {:name generic/name-field
   :tissue_specific_transgenes tissue-specific-transgenes})
