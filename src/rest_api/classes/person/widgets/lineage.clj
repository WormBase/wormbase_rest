(ns rest-api.classes.person.widgets.lineage
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [datomic.api :as d]
    [rest-api.db.main :refer [datomic-conn]]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.date :as date]))

(defn- generate-map [oid]
  (let [other-person-id (get oid 0)
        other-person-name (get oid 1)
        json-data (get oid 2)
        roles (filter #(re-find #"role" (str %)) (keys json-data))
        level
        (for [role roles
              :let
              [level (str/capitalize
                       (str/replace
                         (str/replace
                           (str role) #":role/" "") #"-" " "))]] level)
        duration
        (for [role roles
              :let [rfrom (keyword
                            (str
                              (str/replace 
                                (str/replace 
                                  (str role) #"/" ".") #":" "") "/from"))
;;                     rto (keyword
;;                           (str
;;                             (str/replace 
;;                               (str/replace 
;;                                 (str role) #"/" ".") #":" "") "/to"))
;;                     r (-> role (str/replace #"/" ".") (str/replace #":" ""))
;;                     rto (keyword (str r "/to"))
                    rto (keyword (str (-> role (str/replace #"/" ".") (str/replace #":" "")) "/to"))

                    from (if (contains? (first (json-data role)) rfrom)
                           (date/format-date5 ((first (json-data role)) rfrom))
                           nil )
                    to (if (contains? (first (json-data role)) rto) 
                         (date/format-date5 ((first (json-data role)) rto)) 
                         nil )
                    duration (str from " - " to)]] duration)]
    (pace-utils/vmap
      :level level
      :duration duration
      :name
      (pace-utils/vmap
        :label other-person-name
        :id other-person-id
        :taxonomy "all"
        :class "person"))))

(defn supervised-by [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find ?other-person-id ?other-person-name (pull ?person-supervised-by-ent [*])
                         :in $ ?person
                         :where
                         [?person :person/supervised-by ?person-supervised-by-ent]
                         [?person-supervised-by-ent :person.supervised-by/person ?other-person-ent]
                         [?other-person-ent :person/id ?other-person-id]
                         [?other-person-ent :person/standard-name ?other-person-name]]
                       db (:db/id person))
                  (map (fn [oid]
                         (generate-map oid)))
                  (seq))]
    {:data (if data data)
     :description "people who supervised this person"}))

(defn supervised [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find ?other-person-id ?other-person-name (pull ?person-supervised-by-ent [*])
                         :in $ ?person
                         :where
                         [?other-person-ent :person/supervised-by ?person-supervised-by-ent]
                         [?person-supervised-by-ent :person.supervised-by/person ?person]
                         [?other-person-ent :person/id ?other-person-id]
                         [?other-person-ent :person/standard-name ?other-person-name]]
                       db (:db/id person))
                  (map (fn [oid]
                         (generate-map oid)))
                  (seq))]
    {:data (if data data)
     :description "people supervised by this person"}))

(defn worked-with [person]
  (let [db (d/entity-db person)
        data (->> (d/q '[:find ?other-person-id ?other-person-name (pull ?personworkedwithent [*])
                         :in $ ?person
                         :where
                         [?other-person-ent :person/worked-with ?personworkedwithent]
                         [?personworkedwithent :person.worked-with/person ?person]
                         [?other-person-ent :person/id ?other-person-id]
                         [?other-person-ent :person/standard-name ?other-person-name]]
                       db (:db/id person))
                  (map (fn [oid]
                         (generate-map oid)))
                  (seq))]
    {:data (if data data)
     :description "people with whom this person worked"}))

(def scaling-hash
  (json/parse-string (slurp "http://tazendra.caltech.edu/~azurebrd/cgi-bin/forms/wbpersonLineageScaling.json") true))

(def role-colour
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
  (let [person-id (get oid 0)
        person-name (str/replace (get oid 1) #"'" "")
        other-person-id (get oid 2)
        other-person-name (str/replace (get oid 3) #"'" "")
        json-data (get oid 4)
        roles (filter #(re-find #"role" (str %)) (keys json-data))
        levels
        (for [role roles
              :let
              [levels (str/capitalize
                        (str/replace
                          (str/replace
                            (str/replace
                              (str role) #":role/" "") #"-" " ") #"'" ""))]] levels)]
    (for [level levels
          data (if ((keyword level) role-colour) 
                 (vector
                   (hash-map
                     :person-id person-id
                     :person-name person-name
                     :other-person-id other-person-id
                     :other-person-name other-person-name
                     :level level)))] data)))

(defn- generate-map-this-person [oid]
  (let [person-id (get oid 0)
        person-name (get oid 1)
        data (vector
               (hash-map
                 :person-id person-id
                 :person-name person-name))] data))

(defn- get-this-node-scaling [queried-data]
  (let [value (first queried-data)
        data  (if (nil? value)
                1
                (if (nil? ((keyword (:person-id value)) scaling-hash))
                  1 
                  (parse-int ((keyword (:person-id value)) scaling-hash))))] data))

(defn- get-other-node-scaling [queried-data] 
  (if queried-data
    (for [value queried-data
          :let [data 
                (if (nil? value)
                  1
                  (if (nil? ((keyword (:other-person-id value)) scaling-hash))
                    1 
                    (parse-int ((keyword (:other-person-id value)) scaling-hash))))]] data)))

(defn- filter-this-node [direct-or-full queried-data]
  (let [value (first queried-data)
        scaling (if (nil? ((keyword (:person-id value)) scaling-hash))
                  1 
                  (parse-int ((keyword (:person-id value)) scaling-hash)))
        data (str "id: '" 
                  direct-or-full 
                  (:person-id value) 
                  "', name: '" 
                  (:person-name value) 
                  "', url: '" 
                  (:person-id value) 
                  "', scaling: '" 
                  scaling 
                  "', radius: '100', nodeshape: 'rectangle'")] data))

(defn- filter-other-nodes [direct-or-full largest-node-scaling queried-data]
  (for [value queried-data
        :let [scaling
              (if (nil? value) 
                1
                (if (nil? ((keyword (:other-person-id value)) scaling-hash))
                  1 
                  (parse-int ((keyword (:other-person-id value)) scaling-hash))))
              radius 100
              radius (+ 25 (* 50 (/ (Math/log scaling) (Math/log largest-node-scaling))))
              data 
              (if-not (nil? value) 
                (str "id: '" 
                     direct-or-full
                     (:other-person-id value) 
                     "', name: '" 
                     (:other-person-name value) 
                     "', url: '" 
                     (:other-person-id value) 
                     "', scaling: '" 
                     scaling 
                     "', radius: '" 
                     radius 
                     "', nodeshape: 'ellipse'"))]] data))

(defn- get-existing-roles [queried-data]
  (for [value queried-data
        :let [data (str (:level value))]] data))

(defn- filter-existing-roles [existing-roles-unique]
  (apply merge (for [role existing-roles-unique
                     :let [data  
                           (pace-utils/vmap
                             (keyword role) ((keyword role) role-colour))]] data)))

(defn- filter-edges-supervisees [direct-or-full queried-data]
  (if queried-data
    (for [value queried-data
          :let [data 
                (if-not (nil? value)
                  (str "source: '" 
                       direct-or-full 
                       (:person-id value) 
                       "', target: '" 
                       direct-or-full 
                       (:other-person-id value) 
                       "', role: '" 
                       (:level value) 
                       "', targetArrowShape: 'triangle', lineStyle: 'solid', lineColor: '" 
                       ((keyword (:level value)) role-colour) "'"))]] data)))

(defn- filter-edges-supervisors [direct-or-full queried-data]
  (if queried-data
    (for [value queried-data
          :let [line-style (if (= direct-or-full "Direct") "dashed" "solid")
                data 
                (if-not (nil? value)
                  (str "source: '" 
                       direct-or-full 
                       (:other-person-id value) 
                       "', target: '" 
                       direct-or-full 
                       (:person-id value) 
                       "', role: '" 
                       (:level value) 
                       "', targetArrowShape: 'triangle', lineStyle: '" 
                       line-style 
                       "', lineColor: '" 
                       ((keyword (:level value)) role-colour) "'"))]] data)))

(defn- query-supervisors [person-id]
  (let [db (d/db datomic-conn)
        queried-data-supervisors (->> (d/q '[:find ?person-id ?person-name ?other-person-id ?other-person-name (pull ?person-supervised-by-ent [*])
                                             :in $ ?person-id
                                             :where
                                             [?person :person/id ?person-id]
                                             [?person :person/supervised-by ?person-supervised-by-ent]
                                             [?person :person/standard-name ?person-name]
                                             [?person-supervised-by-ent :person.supervised-by/person ?other-person-ent]
                                             [?other-person-ent :person/id ?other-person-id]
                                             [?other-person-ent :person/standard-name ?other-person-name]]
                                           db person-id)
                                      (map (fn [oid]
                                             (generate-map-nodes-edges oid)))
                                      (flatten))]
    (if queried-data-supervisors queried-data-supervisors)))

(defn- recurse-supervisors [queried-data]
  (if (nil? queried-data) nil
    (for [value queried-data
          :let [supervisors-data (query-supervisors (:other-person-id value)) 
                data (flatten (conj supervisors-data (recurse-supervisors supervisors-data)))]] data)))

(defn- query-supervisees [person-id]
  (let [db (d/db datomic-conn)
        queried-data-supervisees (->> (d/q '[:find ?person-id ?person-name ?other-person-id ?other-person-name (pull ?person-supervised-by-ent [*])
                                             :in $ ?person-id
                                             :where
                                             [?person :person/id ?person-id]
                                             [?other-person-ent :person/supervised-by ?person-supervised-by-ent]
                                             [?person :person/standard-name ?person-name]
                                             [?person-supervised-by-ent :person.supervised-by/person ?person]
                                             [?other-person-ent :person/id ?other-person-id]
                                             [?other-person-ent :person/standard-name ?other-person-name]]
                                           db person-id)
                                      (map (fn [oid]
                                             (generate-map-nodes-edges oid)))
                                      (flatten))]
    (if queried-data-supervisees queried-data-supervisees)))

(defn- recurse-supervisees [queried-data]
  (if (nil? queried-data)
    nil
    (for [value queried-data
          :let [supervisees-data (query-supervisees (:other-person-id value)) 
                data (flatten (conj supervisees-data (recurse-supervisees supervisees-data)))]] data)))

(defn- query-this-person [person]
  (let [db (d/entity-db person)
        queried-data-this-person (->> (d/q '[:find ?person-id ?person-name 
                                             :in $ ?person
                                             :where
                                             [?person :person/standard-name ?person-name]
                                             [?person :person/id ?person-id]]
                                           db (:db/id person))
                                      (map (fn [oid]
                                             (generate-map-this-person oid)))
                                      (flatten))]
    (if queried-data-this-person queried-data-this-person)))

(defn ancestors-data [person]
  (let [db (d/entity-db person)
        queried-data-this-person (query-this-person person)
        this-person-id (:person-id (first queried-data-this-person))
        this-person-supervisors (query-supervisors this-person-id)
        this-person-supervisees (query-supervisees this-person-id)
        edges-direct
        (str "edges: [ { data: { " 
             (str/join " } },{ data: { " 
                       (remove str/blank? 
                               (flatten
                                 (conj (filter-edges-supervisees "Direct" this-person-supervisees) (filter-edges-supervisors "Direct" this-person-supervisors))))) " } } ]")
        existing-roles-direct
        (filter-existing-roles 
          (distinct 
            (flatten 
              (conj (get-existing-roles this-person-supervisors) (get-existing-roles this-person-supervisees)))))
        largest-node-scaling-direct
        (apply max 
               (flatten 
                 (conj (get-other-node-scaling this-person-supervisees) 
                       (conj (get-other-node-scaling this-person-supervisors) (get-this-node-scaling queried-data-this-person)))))
        nodes-direct
        (str "nodes: [ { data: { " 
             (str/join " } },{ data: { " 
                       (remove str/blank? 
                               (flatten 
                                 (conj (filter-other-nodes "Direct" largest-node-scaling-direct this-person-supervisees)
                                       (conj (filter-other-nodes "Direct" largest-node-scaling-direct this-person-supervisors) (filter-this-node "Direct" queried-data-this-person)))))) " } } ]")
        queried-data-recurse-supervisors
        (distinct
          (flatten
            (conj (recurse-supervisors this-person-supervisors) this-person-supervisors )))
        queried-data-recurse-supervisees
        (distinct
          (flatten 
            (conj (recurse-supervisees this-person-supervisees) this-person-supervisees )))
        edges-full
        (str "edges: [ { data: { " 
             (str/join " } },{ data: { " 
                       (flatten 
                         (conj (filter-edges-supervisees "Full" queried-data-recurse-supervisees) (filter-edges-supervisors "Full" queried-data-recurse-supervisors)))) " } } ]")
        existing-roles-full
        (filter-existing-roles 
          (distinct 
            (flatten 
              (conj (get-existing-roles this-person-supervisors) (get-existing-roles queried-data-recurse-supervisees) 
                    (conj (get-existing-roles this-person-supervisors) (get-existing-roles queried-data-recurse-supervisors))))))
        largest-node-scaling-full
        (apply max 
               (flatten 
                 (conj (get-other-node-scaling queried-data-recurse-supervisors) (get-this-node-scaling queried-data-this-person))))
        nodes-full
        (str "nodes: [ { data: { " 
             (str/join " } },{ data: { " 
                       (remove str/blank? 
                               (flatten 
                                 (conj (filter-other-nodes "Full" largest-node-scaling-full queried-data-recurse-supervisees) 
                                       (conj (filter-other-nodes "Full" largest-node-scaling-full queried-data-recurse-supervisors) (filter-this-node "Full" queried-data-this-person))))))" } } ]")
        elements-full (str "{ " nodes-full ", " edges-full " }")
        elements-direct (str "{ " nodes-direct ", " edges-direct " }")]
    {:existingRolesFull existing-roles-full
     :existingRolesDirect existing-roles-direct
     :thisPerson this-person-id 
     :elementsFull (if elements-full elements-full)
     :elementsDirect (if elements-direct elements-direct)
     :description "ancestors_data"}))

(def widget
  {:ancestors_data           ancestors-data
   :supervised               supervised   
   :supervised_by            supervised-by
   :worked_with              worked-with})
