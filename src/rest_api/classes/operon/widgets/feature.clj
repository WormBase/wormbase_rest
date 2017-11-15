(ns rest-api.classes.operon.widgets.feature
  (:require
    [datomic.api :as d]
    [rest-api.classes.feature.core :as feature]
    [rest-api.classes.generic-fields :as generic]))

(defn associated-features [operon]
  (let [db (d/entity-db operon)
        data (->> (d/q '[:find [?f ...]
                         :in $ ?operon
                         :where
                         [?fg :feature.associated-with-operon/operon ?operon]
                         [?f :feature/associated-with-operon ?fg]]
                       db (:db/id operon))
                  (map (partial feature/associated-feature db))
                  (seq))]
    {:data data
     :description "Features associated with this operon"}))

(def widget
  {:name generic/name-field
   :features associated-features})
