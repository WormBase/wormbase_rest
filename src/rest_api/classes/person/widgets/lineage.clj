(ns rest-api.classes.person.widgets.lineage
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [datomic.api :as d]
    [rest-api.db.main :refer [datomic-conn]]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.date :as date]))

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
                           (str role) #":role/" "") #"-" " "))]] level)
        duration
        (for [role roles
              :let [rfrom (keyword
                            (str
                              (clojure.string/replace 
                                (clojure.string/replace 
                                  (str role) #"/" ".") #":" "") "/from"))]
              :let [rto (keyword
                          (str
                            (clojure.string/replace 
                              (clojure.string/replace 
                                (str role) #"/" ".") #":" "") "/to"))]
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

(def scalingHash
  (json/parse-string (slurp "http://tazendra.caltech.edu/~azurebrd/cgi-bin/forms/wbpersonLineageScaling.json") true))

(def roleColour
  (hash-map
    :Phd                            "#B40431"
    :Postdoc                        "#00E300"
    :Masters                        "#FF8000"
    (keyword "Research staff")      "#08298A"
    :Highschool                     "#05C1F0"
    :Undergrad                      "#B58904"))

(defn- parse-int [s]
  (Integer. (re-find #"\d+" s)))

(defn- generate-map-nodes-edges [oid]
  (let [personid (get oid 0)
        personname (str/replace (get oid 1) #"'" "")
        otherpersonid (get oid 2)
        otherpersonname (str/replace (get oid 3) #"'" "")
        jsondata (get oid 4)
        roles (filter #(re-find #"role" (str %)) (keys jsondata))
        levels
        (for [role roles
              :let
              [levels (str/capitalize
                        (str/replace
                          (str/replace
                            (str/replace
                              (str role) #":role/" "") #"-" " ") #"'" ""))]] levels)]
    (for [level levels
          data (if ((keyword level) roleColour) 
                 (vector
                   (hash-map
                     :personid personid
                     :personname personname
                     :otherpersonid otherpersonid
                     :otherpersonname otherpersonname
                     :level level))
                 nil )] data)))

(defn- generate-map-this-person [oid]
  (let [personid (get oid 0)
        personname (get oid 1)
        data (vector
               (hash-map
                 :personid personid
                 :personname personname))] data))

(defn- get-this-node-scaling [queriedData]
  (let [value (first queriedData)
        data  (if (nil? value)
                1
                (if (nil? ((keyword (:personid value)) scalingHash))
                  1 
                  (parse-int ((keyword (:personid value)) scalingHash))))] data))

(defn- get-other-node-scaling [queriedData] 
  (if (empty? queriedData) nil
    (for [value queriedData
          :let [data 
                (if (nil? value)
                  1
                  (if (nil? ((keyword (:otherpersonid value)) scalingHash))
                    1 
                    (parse-int ((keyword (:otherpersonid value)) scalingHash))))]] data)))

(defn- filter-this-node [directOrFull queriedData]
  (let [value (first queriedData)
        scaling (if (nil? ((keyword (:personid value)) scalingHash))
                  1 
                  (parse-int ((keyword (:personid value)) scalingHash)))
        data (str "id: '" directOrFull (:personid value) "', name: '" (:personname value) "', url: '" (:personid value) "', scaling: '" scaling "', radius: '100', nodeshape: 'rectangle'")]
    data))

(defn- filter-other-nodes [directOrFull largestNodeScaling queriedData]
  (for [value queriedData
        :let [scaling (if (nil? value) 
                        1
                        (if (nil? ((keyword (:otherpersonid value)) scalingHash))
                          1 
                          (parse-int ((keyword (:otherpersonid value)) scalingHash))))
              radius 100
              radius (+ 25 (* 50 (/ (Math/log scaling) (Math/log largestNodeScaling))))
              data (if (nil? value) nil (str "id: '" directOrFull(:otherpersonid value) "', name: '" (:otherpersonname value) "', url: '" (:otherpersonid value) "', scaling: '" scaling "', radius: '" radius "', nodeshape: 'ellipse'"))]] data))

(defn- get-existing-roles [queriedData]
  (for [value queriedData
        :let [data (str (:level value))]] data))

(defn- filter-existing-roles [existingRolesUnique]
  (apply merge (for [role existingRolesUnique
                     :let [data  
                           (pace-utils/vmap
                             (keyword role) ((keyword role) roleColour))]] data)))

(defn- filter-edges-supervisees [directOrFull queriedData]
  (if (empty? queriedData)
    nil
    (for [value queriedData
          :let [data 
                (if (nil? value)
                  nil 
                  (str "source: '" directOrFull (:personid value) "', target: '" directOrFull (:otherpersonid value) "', role: '" (:level value) "', targetArrowShape: 'triangle', lineStyle: 'solid', lineColor: '" ((keyword (:level value)) roleColour) "'"))]] data)))

(defn- filter-edges-supervisors [directOrFull queriedData]
  (if (empty? queriedData)
    nil
    (for [value queriedData
          :let [lineStyle (if (= directOrFull "Direct") "dashed" "solid")
                data 
                (if (nil? value)
                  nil 
                  (str "source: '" directOrFull (:otherpersonid value) "', target: '" directOrFull (:personid value) "', role: '" (:level value) "', targetArrowShape: 'triangle', lineStyle: '" lineStyle "', lineColor: '" ((keyword (:level value)) roleColour) "'"))]] data)))

(defn- query-supervisors [personid]
  (let [db (d/db datomic-conn)
        queriedDataSupervisors (->> (d/q '[:find ?personid ?personname ?otherpersonid ?otherpersonname (pull ?personsupervisedbyent [*])
                                           :in $ ?personid
                                           :where
                                           [?person :person/id ?personid]
                                           [?person :person/supervised-by ?personsupervisedbyent]
                                           [?person :person/standard-name ?personname]
                                           [?personsupervisedbyent :person.supervised-by/person ?otherpersonent]
                                           [?otherpersonent :person/id ?otherpersonid]
                                           [?otherpersonent :person/standard-name ?otherpersonname]
                                           ] db personid)
                                    (map (fn [oid]
                                           (generate-map-nodes-edges oid)))
                                    (flatten))]
    (if (empty? queriedDataSupervisors) nil queriedDataSupervisors)))

(defn- recurse-supervisors [queriedData]
  (if (nil? queriedData) nil
    (for [value queriedData
          :let [supervisorsData (query-supervisors (:otherpersonid value)) 
                data (flatten (conj supervisorsData (recurse-supervisors supervisorsData)))]] data)))

(defn- query-supervisees [personid]
  (let [db (d/db datomic-conn)
        queriedDataSupervisees (->> (d/q '[:find ?personid ?personname ?otherpersonid ?otherpersonname (pull ?personsupervisedbyent [*])
                                           :in $ ?personid
                                           :where
                                           [?person :person/id ?personid]
                                           [?otherpersonent :person/supervised-by ?personsupervisedbyent]
                                           [?person :person/standard-name ?personname]
                                           [?personsupervisedbyent :person.supervised-by/person ?person]
                                           [?otherpersonent :person/id ?otherpersonid]
                                           [?otherpersonent :person/standard-name ?otherpersonname]] db personid)
                                    (map (fn [oid]
                                           (generate-map-nodes-edges oid)))
                                    (flatten))]
    (if (empty? queriedDataSupervisees) nil queriedDataSupervisees)))

(defn- recurse-supervisees [queriedData]
  (if (nil? queriedData)
    nil
    (for [value queriedData
          :let [superviseesData (query-supervisees (:otherpersonid value)) 
                data (flatten (conj superviseesData (recurse-supervisees superviseesData)))]] data)))

(defn- query-this-person [person]
  (let [db (d/entity-db person)
        queriedDataThisPerson (->> (d/q '[:find ?personid ?personname 
                                          :in $ ?person
                                          :where
                                          [?person :person/standard-name ?personname]
                                          [?person :person/id ?personid]
                                          ]
                                        db (:db/id person))
                                   (map (fn [oid]
                                          (generate-map-this-person oid)))
                                   (flatten))]
    (if (empty? queriedDataThisPerson) nil queriedDataThisPerson)))

(defn ancestors-data [person]
  (let [db (d/entity-db person)
        queriedDataThisPerson (query-this-person person)
        thisPersonId (:personid (first queriedDataThisPerson))
        thisPersonSupervisors (query-supervisors thisPersonId)
        thisPersonSupervisees (query-supervisees thisPersonId)
        edgesDirect
        (str "edges: [ { data: { " 
             (str/join " } },{ data: { " 
                       (remove str/blank? 
                               (flatten
                                 (conj (filter-edges-supervisees "Direct" thisPersonSupervisees) (filter-edges-supervisors "Direct" thisPersonSupervisors))))) " } } ]")
        existingRolesDirect
        (filter-existing-roles 
          (distinct 
            (flatten 
              (conj (get-existing-roles thisPersonSupervisors) (get-existing-roles thisPersonSupervisees)))))
        largestNodeScalingDirect
        (apply max 
               (flatten 
                 (conj (get-other-node-scaling thisPersonSupervisees) 
                       (conj (get-other-node-scaling thisPersonSupervisors) (get-this-node-scaling queriedDataThisPerson)))))
        nodesDirect
        (str "nodes: [ { data: { " 
             (str/join " } },{ data: { " 
                       (remove str/blank? 
                               (flatten 
                                 (conj (filter-other-nodes "Direct" largestNodeScalingDirect thisPersonSupervisees)
                                       (conj (filter-other-nodes "Direct" largestNodeScalingDirect thisPersonSupervisors) (filter-this-node "Direct" queriedDataThisPerson)))))) " } } ]")
        queriedDataRecurseSupervisors
        (distinct
          (flatten
            (conj (recurse-supervisors thisPersonSupervisors) thisPersonSupervisors )))
        queriedDataRecurseSupervisees
        (distinct
          (flatten 
            (conj (recurse-supervisees thisPersonSupervisees) thisPersonSupervisees )))
        edgesFull
        (str "edges: [ { data: { " 
             (str/join " } },{ data: { " 
                       (flatten 
                         (conj (filter-edges-supervisees "Full" queriedDataRecurseSupervisees) (filter-edges-supervisors "Full" queriedDataRecurseSupervisors)))) " } } ]")
        existingRolesFull
        (filter-existing-roles 
          (distinct 
            (flatten 
              (conj (get-existing-roles thisPersonSupervisors) (get-existing-roles queriedDataRecurseSupervisees) 
                    (conj (get-existing-roles thisPersonSupervisors) (get-existing-roles queriedDataRecurseSupervisors))))))
        largestNodeScalingFull
        (apply max 
               (flatten 
                 (conj (get-other-node-scaling queriedDataRecurseSupervisors) (get-this-node-scaling queriedDataThisPerson))))
        nodesFull
        (str "nodes: [ { data: { " 
             (str/join " } },{ data: { " 
                       (remove str/blank? 
                               (flatten 
                                 (conj (filter-other-nodes "Full" largestNodeScalingFull queriedDataRecurseSupervisees) 
                                       (conj (filter-other-nodes "Full" largestNodeScalingFull queriedDataRecurseSupervisors) (filter-this-node "Full" queriedDataThisPerson))))))" } } ]")
        elementsFull (str "{ " nodesFull ", " edgesFull " }")
        elementsDirect (str "{ " nodesDirect ", " edgesDirect " }")]
    {:existingRolesFull existingRolesFull
     :existingRolesDirect existingRolesDirect
     :thisPerson thisPersonId 
     :elementsFull (if (empty? elementsFull) nil elementsFull)
     :elementsDirect (if (empty? elementsDirect) nil elementsDirect)
     :description "ancestors_data"}))

(def widget
  {:ancestors_data           ancestors-data
   :supervised               supervised   
   :supervised_by            supervised-by
   :worked_with              worked-with})
