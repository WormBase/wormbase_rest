(ns rest-api.classes.transgene.widgets.mapped-transgenes
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [rest-api.db.main :refer [datomic-conn]]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-fns]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn mapped-transgenes [s]
  {:data (let [db (d/db datomic-conn)]
           (->> (d/q '[:find [?tg ...]
                        :in $
                        :where
                        [?tg :transgene/marker-for]]
                      db)
                (map (fn [id]
                       (let [obj (d/entity db id)]
                         {:strain (some->> (:transgene/strain obj)
                                           (map pack-obj))
                          :life_stage (some->> (:expr-pattern/_transgene obj)
                                       (map (fn [ep]
                                              (some->> (:expr-pattern/life-stage ep)
                                                       (map :expr-pattern.anatomy-term/life-stage)
                                                       (map (fn [ls]
                                                              (let [ls-obj (pack-obj ls)]
                                                                {(:id ls-obj) ls-obj}))))))
                                       (flatten)
                                       (into {})
                                       (vals))
                          :transgene (pack-obj obj)
                          :ao (some->> (:expr-pattern/_transgene obj)
                                       (map (fn [ep]
                                              (some->> (:expr-pattern/anatomy-term ep)
                                                       (map :expr-pattern.anatomy-term/anatomy-term)
                                                       (map (fn [ao]
                                                              (let [ao-obj (pack-obj ao)]
                                                                {(:id ao-obj) ao-obj}))))))
                                       (flatten)
                                       (into {})
                                       (vals))
                          :map_position (some->> (:transgene/map obj)
                                                 (map :transgene.map/map)
                                                 (map :map/id)
                                                 (first))
                          :expression_patterns (some->> (:expr-pattern/_transgene obj)
                                                        (map pack-obj))
                          :summary (:transgene.summary/text (first (:transgene/summary obj)))
                          :reference (some->> (:transgene/reference obj)
                                              (map :transgene.reference/paper)
                                              (map pack-obj)
                                              (first))})))))
   :description "tissue-specific genes"})

(def widget
  {:name generic/name-field
   :mapped_transgenes mapped-transgenes})
