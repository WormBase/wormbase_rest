(ns rest-api.classes.transcript.widgets.feature
  (:require
    [datomic.api :as d]
    [rest-api.classes.feature.core :as feature]
    [rest-api.classes.generic-fields :as generic]))

(defn associated-features [transcript]
  (let [db (d/entity-db transcript)
        data  (->>
                (d/q '[:find [?f ...]
                       :in $ ?transcript
                       :where
                       [?fg :feature.associated-with-transcript/transcript ?transcript]
                       [?f :feature/associated-with-transcript ?fg]]
                   db (:db/id transcript))
                (map (partial feature/associated-feature db))
                (seq))]
    {:data (not-empty data)
     :description "Features associated with this transcript"}))

(def widget
    {:name generic/name-field
     :features associated-features})
