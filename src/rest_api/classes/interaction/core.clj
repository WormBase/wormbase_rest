(ns rest-api.classes.interaction.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :refer [pack-obj humanize-ident]]))

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

(def ^:private interactor-role-rules
  '[[(affected ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/affected]]
    [(affected ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/bait]]
    [(affected ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/cis-regulated]]
    [(affected ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/trans-regulated]]

    [(effector ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/cis-regulator]]
    [(effector ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/target]]
    [(effector ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/trans-regulator]]
    [(effector ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/effector]]

    [(non-directed ?itr)
     [?itr :interactor-info/interactor-type :interactor-info.interactor-type/non-directional]]
    ])

(def ^:private interaction-target
  (some-fn :interaction.feature-interactor/feature
           :interaction.interactor-overlapping-gene/gene
           :interaction.molecule-interactor/molecule
           :interaction.rearrangement/rearrangement
           :interaction.other-interactor/text))

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
  '[[(->interaction ?gene ?h ?int)
     [?h :interaction.interactor-overlapping-gene/gene ?gene]
     [?int :interaction/interactor-overlapping-gene ?h]]
    [(->interaction ?feature ?h ?int)
     [?h :interaction.feature-interactor/feature ?feature]
     [?int :interaction/feature-interactor ?h]]
    [(->interaction ?molecule ?h ?int)
     [?h :interaction.molecule-interactor/molecule ?molecule]
     [?int :interaction/molecule-interactor ?h]]
    [(->interaction ?rearrangement ?h ?int)
     [?h :interaction.rearrangement/rearrangement ?rearrangement]
     [?int :interaction/rearrangement ?h]]

    [(interaction-> ?int ?h ?gene)
     [?int :interaction/interactor-overlapping-gene ?h]
     [?h :interaction.interactor-overlapping-gene/gene ?gene]]
    [(interaction-> ?int ?h ?feature)
     [?int :interaction/feature-interactor ?h]
     [?h :interaction.feature-interactor/feature ?feature]]
    [(interaction-> ?int ?h ?molecule)
     [?int :interaction/molecule-interactor ?h]
     [?h :interaction.molecule-interactor/molecule ?molecule]]
    [(interaction-> ?int ?h ?rearrangement)
     [?int :interaction/rearrangement ?h]
     [?h :interaction.rearrangement/rearrangement ?rearrangement]]
    [(interaction-> ?int ?h ?other)
     [?int :interaction/other-interactor ?h]
     [?h :interaction.other-interactor/text ?other]]

    [(x->neighbour ?x ?xh ?neighbour ?nh ?ix)
     (->interaction ?x ?xh ?ix)
     (interaction-> ?ix ?nh ?neighbour)
     [(not= ?x ?neighbour)]]
    [(x->neighbour-non-predicted ?gene ?gh ?neighbour ?nh ?ix)
     (x->neighbour ?gene ?gh ?neighbour ?nh ?ix)
     (not
      [?ix :interaction/type :interaction.type/predicted])]]
  )

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

(def deprecated-interaction-types
  (set [:interaction.type/genetic:asynthetic
        :interaction.type/genetic:complete-mutual-suppression
        :interaction.type/genetic:complete-suppression
        :interaction.type/genetic:complete-unilateral-suppression
        :interaction.type/genetic:enhancement
        :interaction.type/genetic:epistasis
        :interaction.type/genetic:maximal-epistasis
        :interaction.type/genetic:minimal-epistasis
        :interaction.type/genetic:mutual-enhancement
        :interaction.type/genetic:mutual-oversuppression
        :interaction.type/genetic:mutual-suppression
        :interaction.type/genetic:negative-genetic
        :interaction.type/genetic:neutral-epistasis
        :interaction.type/genetic:neutral-genetic
        :interaction.type/genetic:no-interaction
        :interaction.type/genetic:opposing-epistasis
        :interaction.type/genetic:oversuppression
        :interaction.type/genetic:oversuppression-enhancement
        :interaction.type/genetic:partial-mutual-suppression
        :interaction.type/genetic:partial-suppression
        :interaction.type/genetic:partial-unilateral-suppression
        :interaction.type/genetic:phenotype-bias
        :interaction.type/genetic:positive-epistasis
        :interaction.type/genetic:positive-genetic
        :interaction.type/genetic:qualitative-epistasis
        :interaction.type/genetic:quantitative-epistasis
        :interaction.type/genetic:suppression
        :interaction.type/genetic:suppression-enhancement
        :interaction.type/genetic:synthetic
        :interaction.type/genetic:unilateral-enhancement
        :interaction.type/genetic:unilateral-oversuppression
        :interaction.type/genetic:unilateral-suppression]))

(defn- interaction-type-name [interaction]
  (let [itypes (->> (:interaction/type interaction)
                    (remove #(deprecated-interaction-types %))
                    (map name))
        type-name (or (some #(or (re-matches #"^gi-module-three:neutral" %)
                                 (re-matches #"^gi-module-two.*" %)
                                 (re-matches #"^gi-module-three.*" %))
                            itypes)
                      (first itypes))]
    (case type-name
      "physical:proteindna" "physical:protein-DNA"
      "physical:proteinprotein" "physical:protein-protein"
      "physical:proteinrna" "physical:protein-RNA"

      (cond
       ;; Hack to produce the "type-name" when real type-name
       ;; is regulatory.
       ;; TODO: Perhaps the proposed module system should be able to address
       ;;       this and produce a cleaner solution.
       (re-matches #"^regulatory.*" type-name)
       (let [reg-res (some-> (regulatory-result interaction)
                             (name))
             subtype (case reg-res
                       "negative-regulate" "negatively regulates"
                       "positive-regulate" "positively regulates"
                       "does-not-regulate" "does not regulate"
                       "other")]
         (format "regulatory:%s" subtype))

       (or (re-matches #"^genetic.*" type-name)
           (re-matches #"^gi-module-one.*" type-name))
       "genetic:other"

       (and (not= "predicted" type-name)
            (not (re-matches #".+:.+" type-name)))
       (format "%s:other" type-name)

       :default
       type-name))))

(defn gene-direct-interactions [db gene]
  (d/q '[:find ?int ?gh ?nh
         :in $ % ?gene
         :where
         (x->neighbour ?gene ?gh _ ?nh ?int)]
       db int-rules gene))

(defn gene-nearby-interactions [db gene]
  (if-let [neighbours (->>  (d/q '[:find (distinct ?neighbour) .
                                   :in $ % ?gene
                                   :where
                                   (x->neighbour-non-predicted ?gene _ ?neighbour _ ?int)]
                                 db int-rules gene)
                            ;; remove string-based other-interactors that would cause problem in downstream query
                            (filter (complement string?))
                            (seq))]
    (d/q '[:find ?int ?nh ?n2h
           :in $ % [?neighbour1 ...] [?neighbour2 ...]
           :where
           [(not= ?neighbour1 ?neighbour2)]
           (x->neighbour ?neighbour1 ?nh ?neighbour2 ?n2h ?int)]
         db int-rules neighbours neighbours)
    ))

(defn- predicted [int-type role data]
  (if (= int-type "Predicted")
    (if-let [node-predicted (get-in data [:nodes (:id role) :predicted])]
      node-predicted
      1)
    0))

(defn- entity-ident
  ([role]
   (entity-ident role :class))
  ([role role-selector]
   (keyword (role-selector role) "id")))

(defn- annotate-role [data int-type role]
  (let [ident (entity-ident role)]
    (pace-utils/vassoc role
                       :predicted (predicted int-type role data))))

(defn- assoc-interaction [type-name data unpacked]
  (let [key-path [:nodes (:id unpacked)]
        ar (annotate-role data type-name unpacked)]
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

(defn- overlapping-genes [interaction]
   (->> (:interaction/interactor-overlapping-gene interaction)
        (map :interaction.interactor-overlapping-gene/gene)))

(defn- pack-int-roles
  "Pack interaction roles into the expected format."
  [interaction a b direction]
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
              labels (filter identity (map :label packed))]
          (let [result-key (str/trim (str/join " " labels))]
              (when result-key
                (let [result (merge {:type-name type-name
                                     :direction direction} roles)]
                  [result-key result])))))))

(defn- interaction-info [ia holder1 holder2]
  (let [possible-int-types (get ia :interaction/type #{})
          no-interaction :interaction.type/genetic:no-interaction
          lls (or (:interaction/log-likelihood-score ia) 1000)]
      (cond
       (and (<= lls 1.5)
            (possible-int-types :interaction.type/predicted))
       nil

       :default
       (let [{effectors :effector
              affected :affected
              others :other
              associated :associated-product} (group-by interactor-role
                                                        [holder1 holder2])
              pack-iroles (partial pack-int-roles ia)
              roles (cond (and effectors affected)
                          (pack-iroles effectors affected "Effector->Affected")

                          (and others (not (or effectors affected associated)))
                          (pack-iroles [holder1] [holder2] "non-directional")

                          :else nil)]
         (when (seq (remove nil? roles))
           (->> roles
                (vec)
                (into {})
                (vals))))
       )))

(defn- annotate-interactor-roles [data type-name int-roles]
  (->> int-roles
       (map pack-obj)
       (map (partial annotate-role data type-name))))

(defn- pack-papers [papers]
  (->> papers
       (map (partial pack-obj "paper"))
       (vec)))

(defn- edge-key [x y type-name direction phenotype]
  (str/trimr
   (str x " " y " " type-name " " direction " " (:label phenotype))))



(defn- process-obj-interaction
  [data interaction type-name effector affected direction]
  (let [roles [effector affected]
        packed-roles (annotate-interactor-roles data type-name roles)
        [packed-effector packed-affected] packed-roles
        [e-name a-name] (map :id packed-roles)
        papers (:interaction/paper interaction)
        packed-papers (pack-papers papers)
        phenotype (first (interaction-phenotype-kw interaction))
        packed-int (pack-obj "interaction" interaction)
        packed-phenotype (pack-obj "phenotype" phenotype)
        e-key (edge-key e-name a-name type-name direction packed-phenotype)
        a-key (edge-key a-name e-name type-name direction packed-phenotype)
        assoc-int (partial assoc-interaction type-name)
        result (-> data
                   (assoc-in [:types type-name] 1)
                   (assoc-int packed-effector)
                   (assoc-int packed-affected))]
     (let [result* (cond
                      (get-in result [:edges e-key])
                      (update-in-edges result e-key packed-int packed-papers)

                      (and (= direction "non-directional")
                           (get-in result [:edges a-key]))
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
                                 :type type-name}))]
       result*)))

(defn- fill-interaction
  [data [interaction {:keys [type-name effector affected direction]}]]
  (let [roles [effector affected]]
    (cond
      (not-any? interactor roles) data
      (some nil? roles) data
      :default (process-obj-interaction data
                                        interaction
                                        type-name
                                        effector
                                        affected
                                        direction))))

(defn- make-pairwise
  [db [interaction-id holder1-id holder2-id]]
  (let [interaction (d/entity db interaction-id)]
    (->> (interaction-info interaction
                           (d/entity db holder1-id)
                           (d/entity db holder2-id))
         (filter (fn [{:keys [effector affected]}]
                   (and effector affected))) ;refer to issue WormBase/website#6790
         (map vector (repeat interaction)))
    ))


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

(defn build-interactions [db ints]
  (let [data (->> ints
                  (mapcat (partial make-pairwise db))
                  (reduce (fn [result [interaction {:keys [type-name effector affected direction]}]]
                            (process-obj-interaction result
                                                     interaction
                                                     type-name
                                                     effector
                                                     affected
                                                     direction))
                          {}))
        edge-vals (comp vec fixup-citations vals :edges)]
    {:edges (edge-vals data)}))

(defn build-interactions-graph [db ints-direct ints-nearby]
  (let [process-ints (fn [ints & {:keys [nearby?]}]
                       (->> ints
                            (mapcat (partial make-pairwise db))
                            (reduce (fn [result [interaction {:keys [type-name effector affected direction]}]]
                                      (conj result
                                            {:type type-name
                                             :affected (pack-obj affected)
                                             :effector (pack-obj effector)
                                             :direction direction
                                             :nearby nearby?
                                             :throughput (humanize-ident (:interaction/throughput interaction))
                                             :interaction (pack-obj interaction)
                                             :citation (->> (:interaction/paper interaction)
                                                            (first)
                                                            (pack-obj))}))
                                    [])
                            ))]
    {:edges_all (concat (process-ints ints-direct :nearby? false)
                        (process-ints ints-nearby :nearby? true))}))

(defn interactions
  "Produces a data structure suitable for rendering the table listing."
  [gene]
  {:description "genetic and predicted interactions"
   :data (let [db (d/entity-db gene)
               ints (gene-direct-interactions db (:db/id gene))]
           (build-interactions db ints))})

(defn interaction-details
  "Produces a data-structure suitable for rendering a cytoscape graph."
  [gene]
  {:description "addtional nearby interactions"
   :data (let [db (d/entity-db gene)
               ints (gene-direct-interactions db (:db/id gene))
               ints-nearby (gene-nearby-interactions db (:db/id gene))]
           (build-interactions-graph db ints ints-nearby))})

(defn interactor-types
  [gene]
  {:description "data for creating venn diagram of interactions by its type"
   :data (let [db (d/entity-db gene)
               tuples (d/q '[:find ?neighbour ?x
                             :in $ % [?type-set ...] ?gene
                             :where
                             (x->neighbour ?gene ?gh ?neighbour ?nh ?int)
                             (or-join [?gh ?nh]
                                      (and (effector ?gh)
                                           (affected ?nh))
                                      (and (affected ?gh)
                                           (effector ?nh))
                                      (and (non-directed ?gh)
                                           (non-directed ?nh)))
                             (not [?nh :interaction.other-interactor/text _])
                             [?neighbour :gene/id _]
                             [?int :interaction/type ?t]
                             (not [?int :interaction/type :interaction.type/gi-module-three:neutral])
                             (not-join [?int]
                                       [?int :interaction/regulation-result ?rr]
                                       [?rr :interaction.regulation-result/value :interaction.regulation-result.value/does-not-regulate])
                             [?t :db/ident ?tident]
                             [(name ?tident) ?tident-name]
                             [(clojure.string/split ?tident-name #":") [?x]]
                             [(= ?x ?type-set)]]
                           db
                           (concat int-rules
                                   interactor-role-rules)
                           ["physical" "genetic" "regulatory"]
                           (:db/id gene))]
           (->> tuples
                (group-by first)
                (map (fn [[gid tuples-subset]]
                       {:types (map second tuples-subset)
                        :interactor (pack-obj (if (string? gid)
                                                gid
                                                (d/entity db gid)))}))
                (seq))
)})

(def widget
  {:name generic/name-field
   :interactions interactions
   :interactor_types interactor-types})
