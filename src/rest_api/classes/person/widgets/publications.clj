(ns rest-api.classes.person.widgets.publications
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]))


(defn publications [person]
   (let [db (d/entity-db person)
         dataArrayTuples
         (->> (d/q '[:find [?paper ...]
                   :in $ ?person
                   :where [?paperperson :paper.person/person ?person]
                          [?paper :paper/person ?paperperson]
                          [?paper :paper/type ?typeent]
                          (not [?typeent :paper.type/type :paper.type.type/meeting-abstract])
                          ]
                 db (:db/id person))
              (map (fn [oid]
                     (let [paper (d/entity db oid)]
                      (pace-utils/vmap
                       :year (first (str/split (:paper/publication-date paper) #"-"))
                       :object
                        (pace-utils/vmap
                         :taxonomy "all"
                         :class "paper"
                         :label (:paper/brief-citation paper)
                         :id (:paper/id paper))
                       :brief_citation (:paper/brief-citation paper)
                     ))))
              (seq))]
     (def data (group-by :year dataArrayTuples))
     {:data (if (empty? data) nil data)
   :description
   "publications by this person excluding meeting abstracts."}))

(defn meeting-abstracts [person]
   (let [db (d/entity-db person)
         dataArrayTuples
         (->> (d/q '[:find [?paper ...]
                   :in $ ?person
                   :where [?paperperson :paper.person/person ?person]
                          [?paper :paper/person ?paperperson]
                          [?paper :paper/type ?typeent]
                          [?typeent :paper.type/type :paper.type.type/meeting-abstract]
                          ]
                 db (:db/id person))
              (map (fn [oid]
                     (let [paper (d/entity db oid)]
                      (pace-utils/vmap
                       :year (first (str/split (:paper/publication-date paper) #"-"))
                       :object
                        (pace-utils/vmap
                         :taxonomy "all"
                         :class "paper"
                         :label (:paper/brief-citation paper)
                         :id (:paper/id paper))
                       :brief_citation (:paper/brief-citation paper)
                     ))))
              (seq))]
     (def data (group-by :year dataArrayTuples))
     {:data (if (empty? data) nil data)
   :description
   "meeting abstract publications by this person."}))


(def widget
  { :publications             publications
    :meeting_abstracts        meeting-abstracts
  })

