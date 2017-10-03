(ns rest-api.classes.transposon.widgets.feature
  (:require
    [datomic.api :as d]
    [rest-api.classes.feature.core :as feature]
    [rest-api.classes.generic-fields :as generic]))

(defn associated-features [transposon]
  (let [db (d/entity-db transposon)
        data  (->>
                (d/q '[:find [?f ...]
                       :in $ ?transposon
                       :where
                       [?fg :feature.associated-with-transposon/transposon ?transposon]
                       [?f :feature/associated-with-transposon ?fg]]
                   db (:db/id transposon))
                (map (partial feature/associated-feature db))
                (seq))]
    {:data (not-empty data)
     :description "Features associated with this transposon"}))

(def widget
    {:name generic/name-field
     :features associated-features})
