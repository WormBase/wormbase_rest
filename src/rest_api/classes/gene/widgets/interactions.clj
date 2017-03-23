(ns rest-api.classes.gene.widgets.interactions
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :refer [pack-obj]]))

(def sort-by-id (partial sort-by :id))

(def ^:private interaction-phenotype-key :interaction/interaction-phenotype)

(def ^:private interactor-role-map
  {:interactor-info.interactor-type/affected :affected
   :interactor-info.interactor-type/cis-regulated :affected
   :interactor-info.interactor-type/cis-regulator :effector
   :interactor-info.interactor-type/trans-regulated :affected
   :interactor-info.interactor-type/trans-regulator :effector
   :interactor-info.interactor-type/effector :effector})

(def ^:private interaction-targets
  [:interaction.feature-interactor/feature
   :interaction.interactor-overlapping-cds/cds
   :interaction.interactor-overlapping-gene/gene
   :interaction.interactor-overlapping-protein/protein
   :interaction.molecule-interactor/molecule
   :interaction.other-interactor/text
   :interaction.pcr-interactor/pcr-product
   :interaction.rearrangement/rearrangement
   :interaction.sequence-interactor/sequence
   :interaction.variation-interactor/variation])

(def ^:private some-interaction-target
  (some-fn :interaction.feature-interactor/feature
           :interaction.interactor-overlapping-gene/gene
           :interaction.molecule-interactor/molecule
           :interaction.rearrangement/rearrangement))

(def ^:private all-interaction-targets
  (apply juxt interaction-targets))

(def ^:private interactors
  [:interaction/feature-interactor
   :interaction/interactor-overlapping-cds
   :interaction/interactor-overlapping-gene
   :interaction/interactor-overlapping-protein
   :interaction/molecule-interactor])

(def ^:private corresponding-cds?
  (some-fn
   (comp :gene/_corresponding-cds :gene.corresponding-cds/_cds)))

(def some-interaction
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

;; TODO: distinct-by will be in [pseudoace 0.5.0]
(defn distinct-by
  "Returns a lazy sequence of the elements of coll, removing any
  elements that return duplicate values when passed to a function f."
  ([f]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result x]
          (let [fx (f x)]
            (if (contains? @seen fx)
              result
              (do (vswap! seen conj fx)
                  (rf result x)))))))))
  ([f coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                 ((fn [[x :as xs] seen]
                    (when-let [s (seq xs)]
                      (let [fx (f x)]
                        (if (contains? seen fx)
                          (recur (rest s) seen)
                          (cons x (step (rest s) (conj seen fx)))))))
                  xs seen)))]
     (step coll #{}))))

(defn- interactor-role [interactor]
  (let [int-types (:interactor-info/interactor-type interactor)]
    (or (interactor-role-map (first int-types))
        (if (corresponding-cds? (some-interaction-target interactor))
          :associated-product)
        :other)))

(defn- humanize-name [ident]
  (let [hname (-> (name ident)
                  (str/split #":")
                  (last)
                  (str/replace #"-" " ")
                  (str/capitalize))]
    ;; hacks for when hname trick isn't good enough
    (cond
      (= hname "Proteinprotein") "ProteinProtein"
      :default hname)))

(defn- interaction-type [int]
  (->> int :interaction/type first humanize-name))

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
              (gene-neighbour ?gene ?ng)
              (gene-interaction ?ng ?int)]
            db int-rules gene)
       (filter (fn [[_ cnt]]
                 (> cnt 1)))
       (map first)))

(defn- predicted [int-type role data]
  (if (= int-type "Predicted")
    (if-let [node-predicted (get-in data [:nodes (:id role) :predicted])]
      node-predicted
      1)
    0))

(defn- any-interactor-predicted?
  "Return true iif any node has predicted set to 1 for
  any targets of `interaction`."
  [data interaction]
  (let [predictions (->> (all-interaction-targets interaction)
                         (map (fn [obj-id]
                                (get-in data [:nodes obj-id :predicted])))
                         (filter identity))]
    (some #(= (:predicted %) 1) predictions)))

(defn gene-interactions [obj data nearby?]
  (let [db (d/entity-db obj)
        id (:db/id obj)
        ia-ids (concat
                (gene-direct-interactions db id)
                (if nearby?
                  (gene-nearby-interactions db id)))
        interactor-predicted? (partial any-interactor-predicted? data)
        interactions (d/pull-many db ["*"] ia-ids)]
    (if nearby?
      (filter interactor-predicted? interactions)
      interactions)))

(defn- identity-kw [role]
  (keyword (:class role) "id"))

(defn- annotate-role [obj data int-type role]
  (let [identify (identity-kw role)]
    (pace-utils/vassoc role
                       :predicted (predicted int-type role data)
                       :main (if (= (identify obj) (:id role))
                               1))))

(defn- assoc-interaction [obj typ nearby? data unpacked]
  (let [key-path [:nodes (:id unpacked)]]
    (if-not (get-in data key-path)
      (-> data
          (assoc-in key-path (annotate-role obj data typ unpacked))
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

(defn- guess-class [entity-map]
  (->> (keys entity-map)
       (filter #(= (name %) "id"))
       (first)
       (namespace)))

(defn- pack-int-roles
  "Pack interaction roles into the expected format."
  [ref-obj int nearby? a b direction]
  (let [typ (interaction-type int)
        non-directional? (= direction "non-directional")]
    (for [x a
          y b
          :let [xt (some-interaction-target x)
                yt (some-interaction-target y)]
          :when (not= xt yt)]
      (when-not (and nearby?
                     (= (guess-class ref-obj) "interaction")
                     (not-any? #(= % ref-obj) [xt yt]))
        (let [participants (if non-directional?
                             (vec (sort-by-id [xt yt]))
                             [xt yt])
              packed (sort-by-id (map pack-obj participants))
              roles (zipmap [:effector :affected]
                            (sort-by (partial not= ref-obj) [xt yt]))
              labels (filter identity (map :label packed))
              result-key (str/trim (str/join " " labels))]
          (when result-key
            (if-let [result (merge {:typ typ :direction direction} roles)]
              [result-key result])))))))

(defn- interaction-info [ia ref-obj nearby?]
  (let [possible-int-types (get ia :interaction/type #{})
        no-interaction :interaction.type/genetic:no-interaction
        lls (or (:interaction/log-likelihood-score ia) 1000)]
    (cond
      (pace-utils/not-nil? (possible-int-types no-interaction))
      nil

      (and (<= lls 1.5)
           (and (not= (guess-class ref-obj) "interaction")
                (possible-int-types :interaction.type/predicted)))
      nil

      :default
      (let [ia-refs (interactor-refs (d/entity-db ia))
            get-interactors (apply juxt ia-refs)
            interactions (apply concat (get-interactors ia))
            {effectors :effector
             affected :affected
             others :other
             associated :associated-product} (group-by interactor-role
                                                       interactions)
            pack-iroles (partial pack-int-roles ref-obj ia nearby?)
            roles (if (or effectors affected)
                    (let [x (concat effectors others)]
                      (pack-iroles x affected "Effector->Affected"))
                    (pack-iroles others others "non-directional"))]
        (->> roles
             (vec)
             (into {})
             (vals))))))

(defn- annotate-interactor-roles [obj data typ int-roles]
  (->> int-roles
       (map pack-obj)
       (map (partial annotate-role obj data typ))))

(defn- pack-papers [papers]
  (->> papers
       (map (partial pack-obj "paper"))
       (vec)))

(defn- assoc-showall [data nearby?]
  (assoc data :showall (or (< (count (:edges data)) 100) nearby?)))

(defn- edge-key [x y typ phenotype]
  (str/trimr
   (str x " " y " " typ " " (:label phenotype))))

(defn- process-obj-interaction
  [obj nearby? data interaction typ effector affected direction]
  (let [roles [effector affected]
        packed-roles (annotate-interactor-roles obj data typ roles)
        [packed-effector packed-affected] packed-roles
        [e-name a-name] (map :label packed-roles)
        ;; WARNING: could be more than one phenotype
        papers (:interaction/paper interaction)
        packed-papers (pack-papers papers)
        phenotype (first (interaction-phenotype-key interaction))
        packed-int (pack-obj "interaction" interaction)
        packed-phenotype (pack-obj "phenotype" phenotype)
        e-key (edge-key e-name a-name typ packed-phenotype)
        a-key (edge-key a-name e-name typ packed-phenotype)
        assoc-int (partial assoc-interaction obj typ nearby?)
        result (-> data
                   (assoc-in [:types typ] 1)
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
                               :type typ
                               :nearby (if nearby?
                                         "1"
                                         "0")}))]
      (assoc-showall result* nearby?))))

(defn- obj-interaction
  [obj nearby? data [interaction
                     {:keys [typ effector affected direction]}]]
  (let [roles [effector affected]] 
    (cond
      (not-any? some-interaction roles) data
      (not effector) data
      :default (process-obj-interaction obj
                                        nearby?
                                        data
                                        interaction
                                        typ
                                        effector
                                        affected
                                        direction))))

(defn- obj-interactions
  [obj data & {:keys [nearby?]}]
  (let [ints (gene-interactions obj data nearby?)
        interactions (->> ints
                          (mapcat (fn [interaction]
                                    (map vector
                                         (repeat interaction)
                                         (interaction-info interaction
                                                           obj
                                                           nearby?))))
                          (distinct-by #(:interaction/id (first %))))
        mk-interaction (partial obj-interaction obj nearby?)]
    (if (and nearby? (> (count interactions) 3000))
      (assoc data :showall "0")
      (reduce mk-interaction data interactions))))

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

(defn- build-interactions [gene results-formatter]
  (let [edge-vals (comp vec fixup-citations vals :edges)
        data (obj-interactions gene {} :nearby? false)
        edges (edge-vals data)
        results (obj-interactions gene data :nearby? true)
        edges-all (vals (:edges results))]
    (-> results
        (assoc :phenotypes (collect-phenotypes edges-all))
        (assoc :edges edges)
        (assoc :edges_all edges-all)
        (results-formatter))))

(defn- format-interactions [results]
  (if (:showall results)
    (-> results
        (assoc :class "Gene")
        (assoc :showall "1"))
    (select-keys results [:edges])))

(defn- format-interaction-details [results]
  (update-in results [:showall] #(str (if % 1 0))))

(defn interactions
  "Produces a data structure suitable for rendering the table listing."
  [gene]
  {:description "genetic and predicted interactions"
   :data (build-interactions gene format-interactions)})

(defn interaction-details
  "Produces a data-structure suitable for rendering a cytoscape graph."
  [gene]
  {:description "addtional nearby interactions"
   :data (build-interactions gene format-interaction-details)})

(def widget
  {:name generic/name-field
   :interactions interactions})
