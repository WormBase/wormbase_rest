(ns rest-api.classes.person.widgets.lineage
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- generate-map [oid]
  (let [otherpersonid (get oid 0)
        otherpersonname (get oid 1)
        jsondata (get oid 2)
        roles (filter #(re-find #"role" (str %)) (keys jsondata))
        level
        (for [role roles
              :let
              [level (clojure.string/capitalize
                       (clojure.string/replace
                         (clojure.string/replace
                           (str role) #":role/" "") #"-" " ")) ] ] level)
        duration
        (for [role roles
              :let [rfrom (keyword
                            (str
                              (clojure.string/replace 
                                (clojure.string/replace 
                                  (str role) #"/" ".") #":" "") "/from")) ]
              :let [rto (keyword
                          (str
                            (clojure.string/replace 
                              (clojure.string/replace 
                                (str role) #"/" ".") #":" "") "/to")) ]
              :let [from (if (contains? (first (jsondata role)) rfrom)
                           (date/format-date5 ((first (jsondata role)) rfrom))
                           nil )]
              :let [to (if (contains? (first (jsondata role)) rto) 
                         (date/format-date5 ((first (jsondata role)) rto)) 
                         nil )]
              :let [duration (str from " - " to)]] duration)]
    (pace-utils/vmap
      :level level
      :duration duration
      :name
      (pace-utils/vmap
        :label otherpersonname
        :id otherpersonid
        :taxonomy "all"
        :class "person"))))

(defn supervised-by [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find ?otherpersonid ?otherpersonname (pull ?personsupervisedbyent [*])
                         :in $ ?person
                         :where
                         [?person :person/supervised-by ?personsupervisedbyent]
                         [?personsupervisedbyent :person.supervised-by/person ?otherpersonent]
                         [?otherpersonent :person/id ?otherpersonid]
                         [?otherpersonent :person/standard-name ?otherpersonname]
                         ]
                       db (:db/id person))
                  (map (fn [oid]
                         (generate-map oid)))
                  (seq))]
    {:data (if (empty? data) nil data)
     :description "people who supervised this person"}))

(defn supervised [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find ?otherpersonid ?otherpersonname (pull ?personsupervisedbyent [*])
                         :in $ ?person
                         :where
                         [?otherpersonent :person/supervised-by ?personsupervisedbyent]
                         [?personsupervisedbyent :person.supervised-by/person ?person]
                         [?otherpersonent :person/id ?otherpersonid]
                         [?otherpersonent :person/standard-name ?otherpersonname]
                         ]
                       db (:db/id person))
                  (map (fn [oid]
                    (generate-map oid)))
                  (seq))]
    {:data (if (empty? data) nil data)
     :description "people supervised by this person"}))

(defn worked-with [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find ?otherpersonid ?otherpersonname (pull ?personworkedwithent [*])
                         :in $ ?person
                         :where
                         [?otherpersonent :person/worked-with ?personworkedwithent]
                         [?personworkedwithent :person.worked-with/person ?person]
                         [?otherpersonent :person/id ?otherpersonid]
                         [?otherpersonent :person/standard-name ?otherpersonname]
                         ]
                       db (:db/id person))
                  (map (fn [oid]
                    (generate-map oid)))
                  (seq))]
    {:data (if (empty? data) nil data)
     :description "people with whom this person worked"}))

(def widget
  { :supervised_by            supervised-by
    :supervised               supervised   
    :worked_with              worked-with  
   })

