(ns rest-api.classes.pseudogene.widgets.feature
  (:require
    [datomic.api :as d]
    [rest-api.classes.feature.core :as feature]
    [rest-api.classes.generic-fields :as generic]))

(defn associated-features [pseudogene]
  (let [db (d/entity-db pseudogene)
        data  (->>
                (d/q '[:find [?f ...]
                       :in $ ?pseudogene
                       :where
                       [?fg :feature.associated-with-pseudogene/pseudogene ?pseudogene]
                       [?f :feature/associated-with-pseudogene ?fg]]
                   db (:db/id pseudogene))
                (map (partial feature/associated-feature db))
                (seq))]
    {:data (not-empty data)
     :description "Features associated with this pseudogene"}))

(def widget
    {:name generic/name-field
     :features associated-features})
