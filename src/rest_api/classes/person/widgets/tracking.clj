(ns rest-api.classes.person.widgets.tracking
  (:require
    [datomic.api :as d]
    [rest-api.formatters.date :as date]))

(defn last-verified [person]
  {:data
   (if (nil? (:person/last-verified person)) "unknown" (date/format-date4 (:person/last-verified person)))
   :description
   "date curated information last verified."})

(defn status [person]
  {:data (if-let [class (:person/status person)]
           (:status/status class))
   :description (format "current status of the Person:%s %s" (:person/id person) "if not Live or Valid")})

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
  {:last_verified            last-verified
   :status                   status
   :possibly_publishes_as    possibly-publishes-as})
