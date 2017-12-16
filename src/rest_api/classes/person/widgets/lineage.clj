(ns rest-api.classes.person.widgets.lineage
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.date :as date]))

(def role-colour
  {"Phd" "#B40431"
   "Postdoc" "#00E300"
   "Masters" "#FF8000"
   "Research staff" "#08298A"
   "Highschool" "#05C1F0"
   "Undergrad" "#B58904"})

(defn- get-roles [holder]
  (filter #(re-find #"role" (str %)) (keys holder)))

(defn- get-levels [holder]
  (some->> (get-roles holder)
           (map (fn [role]
            (obj/humanize-ident
              (name role))))))

(defn- get-duration [holder]
  (some->> (get-roles holder)
           (map
             (fn [role-key]
               (let [rfrom (keyword
                             (-> role-key
                                 (str/replace #"/" ".")
                                 (str/replace #":" ""))
                             "from")
                     rto (keyword
                           (-> role-key
                               (str/replace #"/" ".")
                               (str/replace #":" ""))
                           "to")
                     role (first (role-key holder))]
                 (str
                   (when-let [from (rfrom role)]
                     (date/format-date5 from))
                   " - "
                   (when-let [to (rto role)]
                     (date/format-date5 to))))))))

(defn- generate-map [holder person]
  {:level (get-levels holder)
   :duration (get-duration holder)
   :keys (keys holder)
   :name (pack-obj person)})

(defn- supervised-by [person]
  (some->> (:person/supervised-by person)
           (map
             (fn [holder]
               (generate-map
                 holder
                 (:person.supervised-by/person holder))))))

(defn- supervised [person]
  (some->> (:person.supervised-by/_person person)
           (map (fn [holder]
                  (generate-map
                    holder
                    (:person/_supervised-by holder))))))

(defn supervised-by-field [person]
   {:data (supervised-by person)
    :description "people who supervised this person"})

(defn supervised-field [person]
  {:data (supervised person)
   :description "people supervised by this person"})

(defn worked-with [person]
  {:data (some->> (:person/worked-with person)
                  (map (fn [holder]
                         (generate-map
                           holder
                           (:person.worked-with/person holder)))))
   :description "people with whom this person worked"})

(defn- parse-int [s]
  (Integer. (re-find #"\d+" s)))

(defn- generate-map-nodes-edges [holder person]
    (some->> (get-roles holder)
             (map (fn [role]
                    (-> role
                        (name)
                        (obj/humanize-ident))))
             (map (fn [level]
                    (if (get role-colour level)
                      (vector
                        (conj
                          (pack-obj person)
                          {:level level})))))))

(defn- get-this-node-scaling [queried-data scaling-map]
  (let [value (first queried-data)]
    (if (or (nil? value)
            (nil? ((keyword (:person-id value)) scaling-map)))
      1
      (parse-int ((keyword (:person-id value)) scaling-map)))))

(defn- get-other-node-scaling [queried-data scaling-map]
  (if queried-data
    (for [value queried-data]
          (if (or (nil? value)
                  (nil? ((keyword (:other-person-id value)) scaling-map)))
            1
            (parse-int ((keyword (:other-person-id value)) scaling-map))))))

(defn- person-node [direct-or-full person scaling-map]
  {:id (str direct-or-full (:person-id person))
   :name (:person/standard-name person)
   :url (:person/id person)
   :scaling (if-let [scaling ((keyword (:person/id person)) scaling-map)]
              (parse-int scaling)
              1)
   :radius 100
   :nodeshape "rectangle"})

(defn- get-node [direct-or-full person largest-node-scaling scaling-map]
  (let [scaling (if-let [scaling ((keyword (:person/id person)) scaling-map)]
                  scaling
                  1)]
      {:id (str direct-or-full (:person/id person))
       :name (:person/standard-name person)
       :url (:person/id person)
       :scaling scaling
;       :radius (+ 25 (* 50 (/ (Math/log scaling) (Math/log largest-node-scaling))))
       :nodeshape "ellipse"}))


(defn- edges-supervisees [direct-or-full person]
  (some->> (:person.supervised-by/_person person)
           (map (fn [holder]
                  (generate-map
                    holder
                    (:person/_supervised-by holder))))
           (map (fn [supervisees]
                  {:source (str direct-or-full (:person/id person))
                   :target (str direct-or-full (:id (:name supervisees)))
                   :role (:level supervisees)
                   :targetArrowShape "triangle"
                   :lineStyle "solid"
                   :lineColor (get role-colour (first (:level supervisees)))}))))

(defn- edges-supervisors [direct-or-full person]
  (some->> (:person/supervised-by person)
           (map (fn [holder]
                  (generate-map
                    holder
                    (:person.supervised-by/person holder))))
           (map
             (fn [supervisor]
               {:source (str direct-or-full (:id (:name supervisor)))
                :target (str direct-or-full (:person/id person))
                :role (first (:level supervisor))
                :targetArrowShape "triangle"
                :lineStyle (if (= direct-or-full "Direct") "dashed" "solid")
                :lineColor (get role-colour (first (:level supervisor)))}))))

(defn recursive-get-supervised [person scaling-map]
  (some->> (:person.supervised-by/_person person)
           (map :person/_supervised-by)
           (map (fn [supervisee]
                  (conj
                    (recursive-get-supervised person scaling-map)
                    (get-node "Full" supervisee 1 scaling-map))))))

(defn recursive-get-supervised-by [person scaling-map]
  (some->> (:person/supervised-by person)
           (map :person.supervised-by/person)
           (map (fn [supervisor]
                  (conj
                    (recursive-get-supervised-by supervisor scaling-map)
                    (get-node "Full" supervisor 1 scaling-map))))))

(defn recursive-get-supervised-roles [person]
  (some->> (:person.supervised-by/_person person)
           (map
             (fn [holder]
              (conj
                (recursive-get-supervised-roles
                  (:person/_supbervised-by holder))
                (first (get-roles holder)))))))

(defn recursive-get-supervised-by-roles [person]
  (some->> (:person/supervised-by person)
           (map
             (fn [holder]
                  (conj
                    (recursive-get-supervised-by-roles
                      (:person.supervised-by/person holder))
                   (first (get-roles holder)))))))

(defn ancestors-data [person]
  (let [scaling-map (json/parse-string (slurp "http://tazendra.caltech.edu/~azurebrd/cgi-bin/forms/wbpersonLineageScaling.json") true)]
    {:existingRolesFull
     (apply
       merge
       (some->> (flatten
                  (flatten
                    (conj
                      (recursive-get-supervised-roles person)
                      (recursive-get-supervised-by-roles person))))
                (map name)
                (map obj/humanize-ident)
                (distinct)
                (map (fn [role]
                       {role (get role-colour role)}))))

     :existingRolesDirect
     (apply
       merge
       (some->> (flatten
                  (flatten
                    (conj
                      (some->> (:person.supervised-by/_person person)
                               (map get-roles)
                               first)
                      (some->> (:person/supervised-by person)
                               (map get-roles)))))
                (map name)
                (map obj/humanize-ident)
                (distinct)
                (map (fn [role]
                       {role (get role-colour role)}))))

     :thisPerson
     (:person/id person)


     :elementsFull
     (when-let [data
                (pace-utils/vmap
                  :edges
                  (some->> (conj
                             (edges-supervisees "Full" person)
                             (edges-supervisors "Full" person))
                           (map (fn [data]
                                  {:data data})))

                  :nodes
                  (remove
                    nil?
                    (some->> (flatten
                               (conj
                                 (vector (person-node "Full" person scaling-map))
                                (recursive-get-supervised person scaling-map)
                                (recursive-get-supervised-by person scaling-map)))
                             (map
                               (fn [data]
                                 (when (some? data)
                                   {:data data}))))))]
       (str/replace (json/generate-string data) #"\"" "'"))

     :elementsDirect
     (when-let [data
                (pace-utils/vmap
                  :edges
                  (some->> (conj
                             (edges-supervisees "Direct" person)
                             (edges-supervisors "Direct" person))
                           (map (fn [data]
                                  {:data data})))

                  :nodes
                  (remove
                    nil?
                    (some->> (flatten
                               (conj
                                 (vector (person-node "Direct" person scaling-map))
                                 (some->> (:person/supervised-by person)
                                          (map :person.supervised-by/person)
                                          (map (fn [supervisor]
                                                 (get-node "Direct" supervisor 1 scaling-map))))
                                 (some->> (:person.supervised-by/_person person)
                                          (map :person/_supervised-by)
                                          (map (fn [supervisee]
                                                 (get-node "Direct" supervisee 1 scaling-map))))))
                             (map
                               (fn [data]
                                 (when (some? data)
                                   {:data data}))))))]
       (str/replace (json/generate-string data) #"\"" "'"))

     :description
     "ancestors_data"}))

(def widget
  {:ancestors_data ancestors-data
   :supervised supervised-field
   :supervised_by supervised-by-field
   :worked_with worked-with})
