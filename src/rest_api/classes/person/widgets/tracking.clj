(ns rest-api.classes.person.widgets.tracking
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.date :as date]))

(defn last-verified [person]
  {:data
   (if (nil? (:person/last-verified person)) "unknown" (date/format-date4 (:person/last-verified person)))
   :description
   "date curated information last verified."})

(defn possibly-publishes-as [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find [?author-name ...]
                         :in $ ?person
                         :where [?person :person/possibly-publishes-as ?author]
                         [?author :author/id ?author-name]]
                       db (:db/id person))
                  (seq))]
    {:data (if (empty? data) nil data)
     :description "other names that the person might publish under"}))

(def widget
  {:last_verified last-verified
   :status generic/status
   :possibly_publishes_as possibly-publishes-as})
