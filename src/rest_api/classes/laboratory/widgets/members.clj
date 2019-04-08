(ns rest-api.classes.laboratory.widgets.members
  (:require
    [datomic.api :as d]
    [clojure.string :as str]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.date :as date]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- generate-map [oid]
  (let [other-person-id (get oid 0)
        other-person-name (get oid 1)
        json-data (get oid 2)
        roles (filter #(re-find #"role" (str %)) (keys json-data))
        level (some->> roles
                       (map obj/humanize-ident))
        from
        (for [role roles
              :let [rfrom (keyword (-> role (str/replace #"/" ".") (str/replace #":" "")) "from")
                    from (if (contains? (first (json-data role)) rfrom)
                           (date/format-date5 ((first (json-data role)) rfrom)))]] from)
        to
        (for [role roles
              :let [ rto (keyword (-> role (str/replace #"/" ".") (str/replace #":" "")) "to")
                    to (if (contains? (first (json-data role)) rto)
                         (date/format-date5 ((first (json-data role)) rto)))]] to)
        duration
        (for [role roles
              :let [rfrom (keyword (-> role (str/replace #"/" ".") (str/replace #":" "")) "from")
                    rto (keyword (-> role (str/replace #"/" ".") (str/replace #":" "")) "to")
                    from (if (contains? (first (json-data role)) rfrom)
                           (date/format-date5 ((first (json-data role)) rfrom)))
                    to (if (contains? (first (json-data role)) rto)
                         (date/format-date5 ((first (json-data role)) rto)))
                    duration (str from " - " to)]] duration)]

    { (keyword other-person-id) {:level (first level)
                                 :person other-person-id
                                 :duration (first duration)
                                 :start_date (first from)
                                 :end_date (first to) } }))


(defn- generate-lineage [laboratory]
  (let [db (d/entity-db laboratory)
        reps (:laboratory/representative laboratory)
        lineage
        (apply merge
               (flatten 
                 (for [rep reps] 
                   (->> (d/q '[:find ?other-person-id ?other-person-name (pull ?person-supervised-by-ent [*])
                               :in $ ?person
                               :where
                               [?other-person-ent :person/supervised-by ?person-supervised-by-ent]
                               [?person-supervised-by-ent :person.supervised-by/person ?person]
                               [?other-person-ent :person/id ?other-person-id]
                               [?other-person-ent :person/standard-name ?other-person-name]]
                             db (:db/id rep))
                        (map generate-map)
                        (seq)))))]
    lineage))

(defn current-members [laboratory]
  (let [db (d/entity-db laboratory)
        lineage (generate-lineage laboratory)
        data (->> (d/q '[:find [?members ...]
                         :in $ ?laboratory
                         :where
                         [?laboratory :laboratory/registered-lab-members ?members]]
                       db (:db/id laboratory))
                  (map (fn [members]
                         (let [member (d/entity db members)
                               keyword-member (keyword (:person/id member))]
                           {:name (pack-obj member)
                            :level (:level (keyword-member lineage))
                            :start_date (:start_date (keyword-member lineage))
                            :end_date (:end_date (keyword-member lineage))
                            :duration (:duration (keyword-member lineage)) })))(seq))]

    {:data data
     :description "current members of the laboratory"}))

(defn former-members [laboratory]
  (let [db (d/entity-db laboratory)
        lineage (generate-lineage laboratory)
        data (->> (d/q '[:find [?members ...]
                         :in $ ?laboratory
                         :where
                         [?laboratory :laboratory/past-lab-members ?members]]
                       db (:db/id laboratory))
                  (map (fn [members]
                         (let [member (d/entity db members)
                               keyword-member (keyword (:person/id member))]
                           {:name (pack-obj member)
                            :level (:level (keyword-member lineage))
                            :start_date (:start_date (keyword-member lineage))
                            :end_date (:end_date (keyword-member lineage))
                            :duration (:duration (keyword-member lineage)) })))(seq))]
    {:data data
     :description "former members of the laboratory"}))

(def widget
  {:name generic/name-field
   :former_members former-members
   :current_members current-members})
