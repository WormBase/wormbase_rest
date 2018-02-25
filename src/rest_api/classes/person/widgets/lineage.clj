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

(def scaling-map (json/parse-string (slurp "http://tazendra.caltech.edu/~azurebrd/cgi-bin/forms/wbpersonLineageScaling.json") true))

(defn- parse-int [s]
  (Integer. (re-find #"\d+" s)))

(defn- get-roles [holder]
  (into
    #{}
    (filter #(re-find #"role" (str %)) (keys holder))))

(defn- get-levels [holder]
  (some->> (get-roles holder)
           (map (fn [role]
            (obj/humanize-ident
              (name role))))))

(defn- get-duration [holder]
  (some->> (get-roles holder)
           (map
             (fn [role-key]
               (let [role-keyword (partial keyword (-> role-key
                                           (str/replace #"/" ".")
                                           (str/replace #":" "")))
                     rfrom (role-keyword "from")
                     rto (role-keyword "to")
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
   :name (pack-obj person)})

(defn- node [direct-or-full person]
  {:id (str direct-or-full (:person-id person))
   :name (:person/standard-name person)
   :url (:person/id person)
   :scaling (if-let [scaling ((keyword (:person/id person)) scaling-map)]
              (parse-int scaling)
              1)
   :radius 100
   :nodeshape "rectangle"})

(defn- get-node [direct-or-full person largest-node-scaling]
  (let [scaling (if-let [scaling ((keyword (:person/id person)) scaling-map)]
                  scaling
                  1)]
      {:id (str direct-or-full (:person/id person))
       :name (:person/standard-name person)
       :url (:person/id person)
       :scaling scaling
       :nodeshape "ellipse"}))

(defn- edge-supervisee [direct-or-full person supervisee]
  {:source (str direct-or-full (:person/id person))
   :target (str direct-or-full (:id (:name supervisee)))
   :role (:level supervisee)
   :targetArrowShape "triangle"
   :lineStyle "solid"
   :lineColor (get role-colour (first (:level supervisee)))})

(defn- edges-supervisees [direct-or-full person]
  (some->> (:person.supervised-by/_person person)
           (map
             (fn [holder]
               (generate-map
                 holder
                 (:person/_supervised-by holder))))
           (map
             (fn [supervisee]
            (edge-supervisee direct-or-full person supervisee)))))

(defn- edge-supervisor [direct-or-full person supervisor]
  {:source (str direct-or-full (:id (:name supervisor)))
   :target (str direct-or-full (:person/id person))
   :role (first (:level supervisor))
   :targetArrowShape "triangle"
   :lineStyle (if (= direct-or-full "Direct") "dashed" "solid")
   :lineColor (get role-colour (first (:level supervisor)))})

(defn- edges-supervisors [direct-or-full person]
  (some->> (:person/supervised-by person)
           (map (fn [holder]
                  (generate-map
                    holder
                    (:person.supervised-by/person holder))))
           (map
             (fn [supervisor]
               (edge-supervisor direct-or-full person supervisor)))))

(defn get-supervisee-graph
  ([person]
   (get-supervisee-graph person #{}))
  ([person visited]
   (let [id (:person/id person)]
     (when (not (contains? visited id))
       (let [visited-new (conj visited id)]
         (if-let [holders (:person.supervised-by/_person person)]
           (let [{nodes :nodes
                  roles :roles
                  edges :edges}
                  (some->> holders
                          (map
                            (fn [holder]
                              (let [supervisee (:person/_supervised-by holder)
                                    role (obj/humanize-ident
                                           (name
                                             (first
                                               (get-roles holder))))
                                    edge {(str id "-" (:person/id supervisee))
                                          (edge-supervisee
                                            "Full"
                                            person
                                            (generate-map holder supervisee))}
                                    {nodes :nodes
                                     roles :roles
                                     edges :edges} (get-supervisee-graph
                                                     supervisee
                                                     visited-new)]
                                {:nodes nodes
                                 :roles (conj roles role)
                                 :edges (merge edges edge)})))
                          (flatten)
                          (apply merge-with into {}))]
              {:nodes (merge {id (node true person)} nodes)
               :roles roles
               :edges edges})
           {:nodes {id (node true person)} ;; leaf nodes
            :roles #{}
            :edges {}}))))))

(defn get-supervisor-graph
  ([person]
   (get-supervisor-graph person #{}))
  ([person visited]
   (let [id (:person/id person)]
     (when (not (contains? visited id))
       (let [visited-new (conj visited id)]
         (if-let [holders (:person/supervised-by person)]
          (let [{nodes :nodes
                  roles :roles
                  edges :edges}
                 (some->> holders
                          (map
                            (fn [holder]
                              (let [supervisor (:person.supervised-by/person holder)
                                    role (obj/humanize-ident
                                           (name
                                             (first
                                               (get-roles holder))))
                                    edge {(str id "-" (:person/id supervisor))
                                          (edge-supervisor
                                            "Full"
                                            person
                                            (generate-map holder supervisor))}
                                    {nodes :nodes
                                     roles :roles
                                     edges :edges} (get-supervisor-graph
                                                     supervisor
                                                     visited-new)]
                                {:nodes nodes
                                 :roles (conj roles role)
                                 :edges (merge edges edge)})))
                          (flatten)
                          (apply merge-with into {}))]
             {:nodes (merge {id (node true person)} nodes)
              :roles roles
              :edges edges}))
           {:nodes {id (node true person)} ;; leaf nodes
            :roles #{}
            :edges {}})))))

(defn- get-graph [person]
  (let [{supervisor-nodes :nodes
         supervisor-roles :roles
         supervisor-edges :edges} (get-supervisor-graph person)

        {supervisee-nodes :nodes
         supervisee-roles :roles
         supervisee-edges :edges} (get-supervisee-graph person)]
    {:nodes (conj supervisor-nodes supervisee-nodes)
     :roles (some? (conj supervisor-roles supervisee-roles))
     :edges (conj supervisor-edges supervisee-edges)}))

(defn- roles-direct [person]
  (some->> (flatten
             (flatten
               (conj
                 (some->> (:person.supervised-by/_person person)
                          (map get-roles)
                          first)
                 (some->> (:person/supervised-by person)
                          (map get-roles)))))
           (apply concat)
           (map name)
           (map obj/humanize-ident)
           (distinct)
           (map (fn [role]
                  {role (get role-colour role)}))
           (apply merge)))

(defn- elements-direct [person]
  (pace-utils/vmap
    :edges
    (some->> (conj
               (edges-supervisees "Direct" person)
               (edges-supervisors "Direct" person))
             (remove nil?)
             (map (fn [data]
                    {:data data})))

    :nodes
    (some->> (flatten
               (conj
                 (vector (node "Direct" person))
                 (some->> (:person/supervised-by person)
                          (map :person.supervised-by/person)
                          (map (fn [supervisor]
                                 (get-node "Direct" supervisor 1))))
                 (some->> (:person.supervised-by/_person person)
                          (map :person/_supervised-by)
                          (map (fn [supervisee]
                                 (get-node "Direct" supervisee 1))))))
             (remove nil?)
             (map
               (fn [data]
                 {:data data})))))

(defn- elements-full [nodes-map edges]
  (pace-utils/vmap
    :edges
    (some->> (vals edges)
             (map (fn [edge]
                    {:data edge})))

    :nodes
    (when-let [nodes (vals nodes-map)]
      (let [largest-node-scaling (some->> nodes
                                          (map :scaling)
                                          (into [])
                                          (apply max))]
      (some->> nodes
               (map
                 (fn [node]
                   (let [scaling (:scaling node)
                         radius (+ 25 (* 50 (/ (Math/log scaling) (Math/log largest-node-scaling))))
                         data (conj {:radius radius}
                                     node)]
                   {:data data}))))))))

(defn ancestors-data [person]
  (let [{nodes :nodes
         roles :roles
         edges :edges} (get-graph person)]
    {:existingRolesFull roles
     :existingRolesDirect (roles-direct person)
     :thisPerson (:person/id person)
     :elementsFull(when-let [elements (elements-full nodes edges)]
                    (str/replace (json/generate-string elements) #"\"" "'"))
     :elementsDirect (when-let [elements (elements-direct person)]
                       (str/replace (json/generate-string elements) #"\"" "'"))
     :description "ancestors_data"}))

(defn supervised-by [person]
  {:data (some->> (:person/supervised-by person)
                  (map
                    (fn [holder]
                      (generate-map holder (:person.supervised-by/person holder)))))
   :description "people who supervised this person"})

(defn supervised [person]
  {:data (some->> (:person.supervised-by/_person person)
           (map (fn [holder]
                  (generate-map holder (:person/_supervised-by holder)))))
   :description "people supervised by this person"})

(defn worked-with [person]
  {:data (some->> (:person/worked-with person)
                  (map (fn [holder]
                         (generate-map
                           holder
                           (:person.worked-with/person holder)))))
   :description "people with whom this person worked"})

(def widget
  {:ancestors_data ancestors-data
   :supervised supervised
   :supervised_by supervised-by
   :worked_with worked-with})
