(ns rest-api.classes.gene.widgets.interactions
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :refer [pack-obj]]))

(def ^:private sort-by-id (partial sort-by :id))

(def ^:private interaction-phenotype-kw
  :interaction/interaction-phenotype)

(def ^:private does-not-regulate-kw
  :interaction.regulation-result.value/does-not-regulate)

(def ^:private interactor-role-map
  {:interactor-info.interactor-type/affected :affected
   :interactor-info.interactor-type/bait :affected
   :interactor-info.interactor-type/cis-regulated :affected
   :interactor-info.interactor-type/cis-regulator :effector
   :interactor-info.interactor-type/effector :effector
   :interactor-info.interactor-type/target :effector
   :interactor-info.interactor-type/trans-regulated :affected
   :interactor-info.interactor-type/trans-regulator :effector})

(def ^:private interaction-target
  (some-fn :interaction.feature-interactor/feature
           :interaction.interactor-overlapping-gene/gene
           :interaction.molecule-interactor/molecule
           :interaction.rearrangement/rearrangement))

(def ^:private interactors
  [:interaction/feature-interactor
   :interaction/interactor-overlapping-cds
   :interaction/interactor-overlapping-gene
   :interaction/interactor-overlapping-protein
   :interaction/molecule-interactor])

(def ^:private corresponding-cds
  (some-fn
   (comp :gene/_corresponding-cds :gene.corresponding-cds/_cds)))

(def interactor
  (some-fn :molecule/id :gene/id :rearrangement/id :feature/id))

(def int-rules
  '[[(gene-interaction ?gene ?int)
     [?ih :interaction.interactor-overlapping-gene/gene ?gene]
     [?int :interaction/interactor-overlapping-gene ?ih]]
    [(gene-neighbour ?gene ?neighbour)
     (gene-interaction ?gene ?ix)
     [?ix :interaction/interactor-overlapping-gene ?ih]
     (not
      [?ix :interaction/type :interaction.type/predicted])
     [?ih :interaction.interactor-overlapping-gene/gene ?neighbour]
     [(not= ?gene ?neighbour)]]])

(defn interactor-idents [db]
  (sort (d/q '[:find [?ident ...]
               :where
               [?e :db/ident ?ident]
               [?e :pace/use-ns "interactor-info"]
               [_ :db/valueType :db.type/ref]
               [_ :db.install/attribute ?e]
               [(namespace ?ident) ?ns]
               [(= ?ns "interaction")]]
             db)))

;; Schema doesn't change once a process is running
(def interactor-refs (memoize interactor-idents))

(defn- interactor-role [interactor]
  (let [int-types (:interactor-info/interactor-type interactor)]
    (or (interactor-role-map (first int-types))
        (if (corresponding-cds (interaction-target interactor))
          :associated-product)
        :other)))

(defn- regulatory-result [interaction]
  (some->> (:interaction/regulation-result interaction)
           (map :interaction.regulation-result/value)
           first))

(defn- humanize-name [ident]
  (let [ident-name (name ident)
        hname (-> (name ident)
                  (str/split #":")
                  (last)
                  (str/replace #"-" " ")
                  (str/capitalize))]
    ;; hacks for when hname trick isn't good enough
    (cond
      (= hname "Proteinprotein") "ProteinProtein"
      :default hname)))

(defn- interaction-type-name [interaction]
  (let [itype (first (:interaction/type interaction))
        type-name (humanize-name itype)]
    (cond
      ;; Hack to produce the "type-name" when real type-name
      ;; is regulatory.
      ;; TODO: Perhaps the proposed module system should be able to address
      ;;       this and produce a cleaner solution.
      (str/includes? (str/lower-case type-name) "regulat")
      (when-let [reg-res (regulatory-result interaction)]
        (let [reg-res-match (re-seq #"^(.*tive)-regulate" (name reg-res))]
          (cond
            (= reg-res does-not-regulate-kw)
            "Does Not Regulate"
            reg-res-match
            (let [term (last (flatten reg-res-match))]
              (str (str/capitalize term) "ly Regulates"))
            :default type-name))
	type-name)
      :default
      type-name)))

(defn gene-direct-interactions [db gene]
  (d/q '[:find [?int ...]
         :in $ % ?gene
         :where
         (gene-interaction ?gene ?int)]
       db int-rules gene))

(defn gene-nearby-interactions [db gene]
  (->> (d/q '[:find ?int (count ?ng)
              :in $ % ?gene
              :where
              (gene-interaction ?ng ?int)
              (gene-neighbour ?gene ?ng)]
            db int-rules gene)
       (filter (fn [[_ cnt]]
                 (> cnt 1)))
       (map first)))

(defn gene-neighbours? [interaction gene-1 gene-2]
  (let [db (d/entity-db gene-1)
        [ix-id g1-id g2-id] (map :db/id [interaction gene-1 gene-2])]
    (when (and g1-id g2-id)
      (d/q '[:find (count ?ng) .
             :in $ % ?int ?gene ?ng
             :where
             (gene-interaction ?ng ?int)
             (gene-neighbour ?gene ?ng)]
             db
             int-rules
             ix-id
             g1-id
             g2-id))))

(defn- predicted [int-type role data]
  (if (= int-type "Predicted")
    (if-let [node-predicted (get-in data [:nodes (:id role) :predicted])]
      node-predicted
      1)
    0))

(defn gene-interactions [obj data nearby?]
  (let [db (d/entity-db obj)
        id (:db/id obj)
        ia-ids (concat
                (gene-direct-interactions db id)
                (if nearby?
                  (gene-nearby-interactions db id)))
        interactions (map (partial d/entity db) ia-ids)]
    interactions))

(defn- entity-ident
  ([role]
   (entity-ident role :class))
  ([role role-selector]
   (keyword (role-selector role) "id")))

(defn- annotate-role [obj data int-type role]
  (let [ident (entity-ident role)]
    (pace-utils/vassoc role
                       :predicted (predicted int-type role data)
                       :main (if (= (ident obj) (:id role))
                               1))))

(defn- assoc-interaction [obj type-name nearby? data unpacked]
  (let [key-path [:nodes (:id unpacked)]
        ar (annotate-role obj data type-name unpacked)]
    (if (and (nil? (get-in data key-path)) ar)
      (-> data
          (assoc-in key-path ar)
          (assoc-in [:ntypes (:class unpacked)] 1))
      data)))

(defn- update-in-uniq [data path func value]
  (update-in data
             path
             (fn [old new]
               (->> (func old new)
                    (set)
                    (sort-by-id)
                    (vec)))
             value))

(defn- update-in-edges
  "Updates interaction `packed-int` and `papers` to a unique
  collection within the edges data structure."
  [data int-key packed-int papers]
  (-> data
      (update-in-uniq [:edges int-key :interactions]
                      conj
                      packed-int)
      (update-in-uniq [:edges int-key :citations]
                      into
                      papers)))

(defn- fixup-citations [edges]
  (map (fn [edge]
         (let [citations (:citations edge)
               interactions (:interactions edge)
               n-interactions (count interactions)
               citations* (if (and (> n-interactions 1)
                                   (= (count citations) 1))
                            (->> (first citations)
                                 (repeat n-interactions)
                                 (vec))
                            citations)]
           (merge edge {:citations citations*})))
       edges))

(defn- entity-type [entity-map]
  (some->> (keys entity-map)
           (filter #(= (name %) "id"))
           (first)
           (namespace)))

(defn- involves-focus-gene? [focus-gene interactors nearby?]
  (let [ident-vals (map :gene/id interactors)
        ids (->> ident-vals (remove nil?) vec)
        focused? (some #{(:gene/id focus-gene)} ids)]
    (when focused?
      ids)))

(defn- overlapping-genes [interaction]
   (->> (:interaction/interactor-overlapping-gene interaction)
        (map :interaction.interactor-overlapping-gene/gene)))

(defn- pack-int-roles
  "Pack interaction roles into the expected format."
  [ref-obj interaction nearby? a b direction]
  (let [type-name (interaction-type-name interaction)
        non-directional? (= direction "non-directional")]
    (for [x a
          y b]
        (let [xt (interaction-target x)
              yt (interaction-target y)
              participants (if non-directional?
                             (vec (sort-by-id [xt yt]))
                             [xt yt])
              packed (map pack-obj participants)
              roles (zipmap [:effector :affected] participants)
              labels (filter identity (map :label packed))
              ix-gene-neigbours? (partial gene-neighbours? interaction ref-obj)
              participant-ids (involves-focus-gene? ref-obj participants nearby?)
              includes-focus-gene? (seq participant-ids)]
          (cond
            (= xt yt) nil

            ;; Indirect interactions (= nearby? true)
            (and nearby?
                 (some nil? (map ix-gene-neigbours? [xt yt])))
             nil

            ;; Direct interactions  (= nearby? false)
            (and (not nearby?)
                 (not= (entity-type ref-obj) "interaction")
                 (not includes-focus-gene?))
            nil
            :default
            (let [result-key (str/trim (str/join " " labels))]
              (when result-key
                (let [result (merge {:type-name type-name
                                     :direction direction} roles)]
                  [result-key result]))))))))

(defn- interaction-info [ia ref-obj nearby?]
  (let [possible-int-types (get ia :interaction/type #{})
        no-interaction :interaction.type/genetic:no-interaction
        focus-gene-id (:gene/id ref-obj)
        lls (or (:interaction/log-likelihood-score ia) 1000)]
    (cond
      (and (<= lls 1.5)
           (not= (entity-type ref-obj) "interaction")
           (possible-int-types :interaction.type/predicted))
      nil

      :default
      (let [ia-refs (interactor-refs (d/entity-db ia))
            get-interactors (apply juxt ia-refs)
            interactors (apply concat (get-interactors ia))
            {effectors :effector
             affected :affected
             others :other
             associated :associated-product} (group-by interactor-role
                                                       interactors)
            pack-iroles (partial pack-int-roles ref-obj ia nearby?)
            roles (if (or effectors affected)
                    (-> (concat effectors others)
                        (pack-iroles affected "Effector->Affected"))
                    (pack-iroles others others "non-directional"))]
        (when (seq (remove nil? roles))
          (->> roles
               (vec)
               (into {})
               (vals)))))))

(defn- annotate-interactor-roles [obj data type-name int-roles]
  (->> int-roles
       (map pack-obj)
       (map (partial annotate-role obj data type-name))))

(defn- pack-papers [papers]
  (->> papers
       (map (partial pack-obj "paper"))
       (vec)))

(defn- assoc-showall [data nearby?]
  (assoc data :showall (or (< (count (:edges data)) 100) nearby?)))

(defn- edge-key [x y type-name phenotype]
  (str/trimr
   (str x " " y " " type-name " " (:label phenotype))))

(defn- process-obj-interaction
  [obj nearby? data interaction type-name effector affected direction]
  (let [roles [effector affected]
        packed-roles (annotate-interactor-roles obj data type-name roles)
        [packed-effector packed-affected] packed-roles
        [e-name a-name] (map :label packed-roles)
        papers (:interaction/paper interaction)
        packed-papers (pack-papers papers)
        phenotype (first (interaction-phenotype-kw interaction))
        packed-int (pack-obj "interaction" interaction)
        packed-phenotype (pack-obj "phenotype" phenotype)
        e-key (edge-key e-name a-name type-name packed-phenotype)
        a-key (edge-key a-name e-name type-name packed-phenotype)
        assoc-int (partial assoc-interaction obj type-name nearby?)
        result (-> data
                   (assoc-in [:types type-name] 1)
                   (assoc-int packed-effector)
                   (assoc-int packed-affected))]
     (let [result* (cond
                      (get-in result [:edges e-key])
                      (update-in-edges result e-key packed-int packed-papers)

                      (get-in result [:edges a-key])
                      (update-in-edges result a-key packed-int packed-papers)

                      :default
                      (assoc-in result
                                [:edges e-key]
                                {:affected packed-affected
                                 :citations packed-papers
                                 :direction direction
                                 :effector packed-effector
                                 :interactions [packed-int]
                                 :phenotype packed-phenotype
                                 :type type-name
                                 :nearby (if nearby? "1" "0")}))]
        (assoc-showall result* nearby?))))

(defn- obj-interaction
  [obj nearby? data [interaction
                     {:keys [type-name effector affected direction]}]]
  (let [roles [effector affected]]
    (cond
      (not-any? interactor roles) data
      (some nil? roles) data
      :default (process-obj-interaction obj
                                        nearby?
                                        data
                                        interaction
                                        type-name
                                        effector
                                        affected
                                        direction))))

(defn- obj-interactions
  [obj data & {:keys [nearby?]}]
  (let [ints (gene-interactions obj data nearby?)
        mk-interaction (partial obj-interaction obj nearby?)
        mk-pair-wise (fn [interaction]
                       (map vector
                            (repeat interaction)
                            (interaction-info interaction obj nearby?)))]
    (if (and nearby? (> (count ints) 3000))
      (assoc data :showall "0")
      (reduce mk-interaction data (mapcat mk-pair-wise ints)))))

(defn- collect-phenotypes
  "Collect phenotypes from node edges."
  [edges]
  (->> edges
       (map :phenotype)
       (filter identity)
       (set)
       (map (fn [pt]
              [(:id pt) pt]))
       (into {})
       (not-empty)))

(defn- build-interactions [gene arrange-results]
  (let [edge-vals (comp vec fixup-citations vals :edges)
        data (obj-interactions gene {} :nearby? false)
        edges (edge-vals data)
        results (obj-interactions gene data :nearby? true)
        edges-all (edge-vals results)]
    (-> results
        (assoc :phenotypes (collect-phenotypes edges-all))
        (arrange-results edges edges-all))))

(defn- arrange-interactions [results edges edges-all]
  (if (:showall results)
    (-> (assoc results :edges edges)
        (assoc :edges_all edges-all)
        (assoc :class "Gene")
        (assoc :showall "1"))
    {:edges edges}))

(defn- arrange-interaction-details [results edges edges-all]
  (-> results
      (assoc :edges edges-all)
      (update-in [:showall] #(str (if % 1 0)))))

(defn interactions
  "Produces a data structure suitable for rendering the table listing."
  [gene]
  {:description "genetic and predicted interactions"
   :data (build-interactions gene arrange-interactions)})

(defn interaction-details
  "Produces a data-structure suitable for rendering a cytoscape graph."
  [gene]
  {:description "addtional nearby interactions"
   :data (build-interactions gene arrange-interaction-details)})

(def widget
  {:name generic/name-field
   :interactions interactions})
