(ns rest-api.classes.person.widgets.lineage
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.date :as date]))

(def scaling-map (json/parse-string (slurp "http://tazendra.caltech.edu/~azurebrd/cgi-bin/forms/wbpersonLineageScalingIntegers.json") true))

(def role-colour
  {"Phd" "#B40431"
   "Postdoc" "#00E300"
   "Masters" "#FF8000"
   "Research staff" "#08298A"
   "Highschool" "#05C1F0"
   "Undergrad" "#B58904"})

(defn- role-has-colour [role]
  (if (get role-colour role)
    true
    false))

(defn- get-roles [holder]
  (into
    #{}
    (filter #(re-find #"role" (str %)) (keys holder))))

(defn- has-a-role-with-colour [holder]
  (if-let [roles (some->> (get-roles holder)
                          (map name)
                          (map obj/humanize-ident)
                          (filter role-has-colour))]
    true
    false))

(defn- generate-map [holder person]
  (some->> (get-roles holder)
           (map
             (fn [role-key]
               {:level (obj/humanize-ident (name role-key))
                :duration (let [role-keyword (partial keyword (-> role-key
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
                                (date/format-date5 to))))
                :name (pack-obj person)}))
           (filter (fn [r] (role-has-colour (:level r))))))

(defn- node [direct-or-full person is-primary]
  {:id (str direct-or-full (:person/id person))
   :name (:person/standard-name person)
   :url (:person/id person)
   :scaling (if-let [scaling ((keyword (:person/id person)) scaling-map)]
              scaling
              1)
   :radius 100
   :nodeshape (if (= is-primary true)
                "rectangle"
                "ellipse")})

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
   :lineColor (get role-colour (:level supervisee))})

(defn- edges-supervisees [direct-or-full person]
  (some->> (:person.supervised-by/_person person)
           (map
             (fn [holder]
               (generate-map
                 holder
                 (:person/_supervised-by holder))))
           (flatten)
           (map
             (fn [supervisee]
               (edge-supervisee direct-or-full person supervisee)))))

(defn- edge-supervisor [direct-or-full person supervisor]
  {:source (str direct-or-full (:id (:name supervisor)))
   :target (str direct-or-full (:person/id person))
   :role (:level supervisor)
   :targetArrowShape "triangle"
   :lineStyle (if (= direct-or-full "Direct") "dashed" "solid")
   :lineColor (get role-colour (:level supervisor))})

(defn- edges-supervisors [direct-or-full person]
  (some->> (:person/supervised-by person)
           (map (fn [holder]
                  (generate-map
                    holder
                    (:person.supervised-by/person holder))))
           (flatten)
           (map
             (fn [supervisor]
               (edge-supervisor direct-or-full person supervisor)))))

(defn get-supervise-graph
  ([person direct-or-full direction]
   (get-supervise-graph person direct-or-full direction #{} true))
  ([person direct-or-full direction visited is-primary]
   (let [id (:person/id person)]
     (when (not (contains? visited id))
       (let [visited-new (conj visited id)]
         (if-let [holders (when (or (= direct-or-full "Full")
                                    (and (= direct-or-full "Direct")
                                         (empty? visited)))
                            (if (= direction "backward")
                              (:person/supervised-by person)
                              (:person.supervised-by/_person person)))]
           (let [{nodes :nodes
                  roles :roles
                  edges :edges}
                 (some->> holders
                          (map
                            (fn [holder]
                              (let [supervise (if (= direction "backward")
                                                (:person.supervised-by/person holder)
                                                (:person/_supervised-by holder))
                                    roles-map (some->> (get-roles holder)
                                                       (map name)
                                                       (map obj/humanize-ident)
                                                       (filter role-has-colour)
                                                       (map (fn [n]
                                                              {n n}))
                                                       (into {}))
                                    edges-map {(str id "-" (:person/id supervise))
                                               (some->> (generate-map holder supervise)
                                                        (map (fn [supervise-map]
                                                               (if (= direction "backward")
                                                                 (edge-supervisor direct-or-full person supervise-map)
                                                                 (edge-supervisee direct-or-full person supervise-map)))))}
                                    {nodes :nodes
                                     roles :roles
                                     edges :edges} (if (empty? roles-map)
                                                     {:nodes {}
                                                      :roles {}
                                                      :edges {}}
                                                     (get-supervise-graph
                                                       supervise
                                                       direct-or-full
                                                       direction
                                                       visited-new
                                                       false))]
                                {:nodes nodes
                                 :roles (merge roles roles-map)
                                 :edges (merge edges edges-map)})))
                          (flatten)
                          (apply merge-with into {}))]
             {:nodes (merge {id (node direct-or-full person is-primary)} nodes)
              :roles roles
              :edges edges})
           {:nodes {id (node direct-or-full person is-primary)} ;; leaf nodes
            :roles {}
            :edges {}}))))))

(defn- get-graph [person direct-or-full]
  (let [{supervisor-nodes :nodes
         supervisor-roles :roles
         supervisor-edges :edges} (get-supervise-graph person direct-or-full "backward")

        {supervisee-nodes :nodes
         supervisee-roles :roles
         supervisee-edges :edges} (get-supervise-graph person direct-or-full "forward")]
    {:nodes (conj supervisor-nodes supervisee-nodes)
     :roles (some->> (conj (keys supervisor-roles) (keys supervisee-roles))
                     (flatten)
                     (distinct)
                     (map (fn [role]
                            {role (get role-colour role)}))
                     (into {}))
     :edges (conj supervisor-edges supervisee-edges)}))

(defn- scale-nodes-map [nodes direct-or-full]
  (when (some? nodes)
    (let [largest-node-scaling (some->> nodes
                                        (map :scaling)
                                        (into [])
                                        (apply max))]
      (some->> nodes
               (map
                 (fn [node]
                   (let [scaling (:scaling node)
                         radius (+ 25 (* 50 (/ (Math/log scaling) (Math/log largest-node-scaling))))
                         data (if (= (:nodeshape node) "rectangle")
                                node
                                (conj node {:radius radius}))]
                     {:data data})))))))

(defn- elements [nodes-map edges-map direct-or-full]
  (pace-utils/vmap
    :edges
    (some->> (vals edges-map)
             (map (fn [edges]
                    (some->> edges
                             (map (fn [edge]
                                    {:data edge})))))
             (flatten))
    :nodes (scale-nodes-map (vals nodes-map) direct-or-full)))

(defn- generate-json-like-string [elements]
  (str/replace
    (str/replace
      (str/replace
        (json/generate-string elements)
        #"\"(\w+)\":" "$1: ")
      #"," ", ")
    #"\"" "'"))

(defn ancestors-data [person]
  (let [{nodes-full :nodes
         roles-full :roles
         edges-full :edges} (get-graph person "Full")
        {nodes-direct :nodes
         roles-direct :roles
         edges-direct :edges} (get-graph person "Direct")]
    {:existingRolesFull roles-full
     :existingRolesDirect roles-direct
     :thisPerson (:person/id person)
     :elementsFull (when-let [elements (elements nodes-full edges-full "Full")]
                     (generate-json-like-string elements))
     :elementsDirect (when-let [elements (elements nodes-direct edges-direct "Direct")]
                       (generate-json-like-string elements))
     :description "ancestors_data"}))

(defn supervised-by [person]
  {:data (some->> (:person/supervised-by person)
                  (map
                    (fn [holder]
                      (generate-map holder (:person.supervised-by/person holder))))
                  (flatten))
   :description "people who supervised this person"})

(defn supervised [person]
  {:data (some->> (:person.supervised-by/_person person)
                  (map (fn [holder]
                         (generate-map holder (:person/_supervised-by holder))))
                  (flatten))
   :description "people supervised by this person"})

(defn worked-with [person]
  {:data (some->> (:person/worked-with person)
                  (map (fn [holder]
                         (generate-map
                           holder
                           (:person.worked-with/person holder))))
                  (flatten))
   :description "people with whom this person worked"})

(def widget
  {:ancestors_data ancestors-data
   :supervised supervised
   :supervised_by supervised-by
   :worked_with worked-with})
