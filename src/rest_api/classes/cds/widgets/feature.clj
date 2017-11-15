(ns rest-api.classes.cds.widgets.feature
  (:require
    [datomic.api :as d]
    [rest-api.classes.feature.core :as feature]
    [rest-api.classes.generic-fields :as generic]))

(defn associated-features [cds]
  (let [db (d/entity-db cds)
        data (->> (d/q '[:find [?f ...]
                         :in $ ?cds
                         :where
                         [?fg :feature.associated-with-cds/cds ?cds]
                         [?f :feature/associated-with-cds ?fg]]
                       db (:db/id cds))
                  (map (partial feature/associated-feature db))
                  (seq))]
    {:data data
     :description "Features associated with this CDS"}))

(def widget
  {:name generic/name-field
   :features associated-features})
