(ns rest-api.classes.gene.widgets.interactions
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :refer [obj-get obj-name pack-obj]]
   [rest-api.classes.gene.generic :as generic]))

(def ^:private interactor-role-map
  {:interactor-info.interactor-type/effector :effector
   :interactor-info.interactor-type/affected :affected
   :interactor-info.interactor-type/trans-regulator :effector
   :interactor-info.interactor-type/cis-regulatory :effector
   :interactor-info.interactor-type/trans-regulated :affected
   :interactor-info.interactor-type/cis-regulated :affected})

(def ^:private interaction-target
  (some-fn
   :interaction.interactor-overlapping-gene/gene
   :interaction.interactor-overlapping-cds/cds
   :interaction.interactor-overlapping-protein/protein
   :interaction.feature-interactor/feature))

(def ^:private some-cds
  (some-fn
   (comp :gene/_corresponding-cds :gene.corresponding-cds/_cds)))

(def some-interaction
  (some-fn :molecule/id :gene/id :rearrangement/id :feature/id))

(defn- interactor-role [interactor]
  (let [int-types (:interactor-info/interactor-type interactor)]
    (or (interactor-role-map (first int-types))
        (if (some-cds (interaction-target interactor))
          :associated-product)
        :other)))

(defn- humanize-name [ident]
  (-> (name ident)
      (str/split #":")
      (last)
      (str/replace #"-" " ")
      (str/capitalize)))

(defn- interaction-type [int]
  (humanize-name (first (:interaction/type int))))

(defn- pack-int-roles [ref-obj int nearby? pred a b direction]
  (for [x a
        y b
        :let [xt (interaction-target x)
              yt (interaction-target y)]
        :when (and (pred xt yt)
                   (or nearby? (= xt ref-obj) (= yt ref-obj)))]
    [(str (:label (pack-obj xt)) " " (:label (pack-obj yt)))
     {:type (interaction-type int)
      :effector xt
      :affected yt
      :direction direction}]))

(defn- interaction-info [int ref-obj nearby?]
  (if (or (not ((:interaction/type int) :interaction.type/predicted))
          (> (or (:interaction/log-likelihood-score int) 1000) 1.5))
    (let [{effectors :effector
           affected :affected
           others :other
           associated :associated-product}
          (group-by interactor-role
                    (concat
                     (:interaction/interactor-overlapping-cds int)
                     (:interaction/interactor-overlapping-gene int)
                     (:interaction/interactor-overlapping-protein int)
                     (:interaction/feature-interactor int)))
          typ (interaction-type int)
          pack-iroles (partial pack-int-roles ref-obj int nearby?)]
      (->> (if (or effectors affected)
             (let [x (concat effectors others)
                   pred (constantly true)]
               (pack-iroles pred x affected "Effector->Affected"))
             (pack-iroles #(not= %1 %2) others others "non-directional"))
           (into {})
           (vals)))))

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
     [(not= ?gene ?neighbour)]]

    ;; Not actually used since count trick is subtantially faster.
    [(gene-neighbour-interaction ?gene ?int)
     (gene-neighbour ?gene ?n1)
     (gene-interaction ?n1 ?int)
     [?int :interaction/interactor-overlapping-gene ?ih]
     [?ih :interaction.interactor-overlapping-gene/gene ?n2]
     [(not= ?n1 ?n2)]
     (gene-neighbour ?gene ?n2)]])

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

(defn gene-interactions [obj nearby?]
  (let [db (d/entity-db obj)
        id (:db/id obj)]
    (map
     (partial d/entity db)
     (concat
      (gene-direct-interactions db id)
      (if nearby?
        (gene-nearby-interactions db id))))))

(defn- assoc-interaction [obj typ data unpacked]
  (let [packed (pack-obj unpacked)]
    (-> data
        (assoc-in [:nodes (:id packed)]
                  (pace-utils/vassoc
                   packed
                   :predicted (if (= typ "Predicted")
                                1
                                0)
                   :main (if (= unpacked obj)
                           1)))
        (assoc-in [:ntypes (:class packed)] 1))))

(defn- update-in-edges [data int-key packed-int papers]
  (-> data
      (update-in [:edges int-key :interactions]
                 conj packed-int)
      (update-in [:edges int-key :citations]
                 into papers)))

(defn- obj-interaction
  [obj data [interaction {:keys [typ effector affected direction]}]]
  (if (every? pace-utils/not-nil? (map some-interaction [effector affected]))
    (let [packed (map pack-obj [effector affected])
          [packed-effector packed-affected] packed
          int-pheno-key :interaction/interaction-phenotype
          [ename aname] (map :label packed)
          ;; warning: could be more than one.
          phenotype (pack-obj "phenotype" (first (int-pheno-key interaction)))
          mk-key #(str %1 " " %2 " " type " " (:label phenotype))
          ;; interactions with the same endpoints but
          ;; a different phenotype get a separate row.
          e-key (mk-key ename aname)
          a-key (mk-key aname ename)
          i-label (str/join " : " (sort [ename aname]))
          packed-int (pack-obj "interaction" interaction :label i-label)
          paper-refs (:interaction/paper interaction)
          papers (map (partial pack-obj "paper") paper-refs)
          assoc-int (partial assoc-interaction obj typ)
          result (-> data
                     (assoc-in [:types typ] 1)
                     (assoc-int effector)
                     (assoc-int affected)
                     (update-in [:phenotypes]
                                pace-utils/vassoc
                                (:id phenotype)
                                phenotype))]
      (cond
        (get-in result [:edges e-key])
        (update-in-edges result e-key packed-int papers)

        (get-in result [:edges a-key])
        (update-in-edges result a-key packed-int papers)

        :default
        (assoc-in result
                  [:edges e-key]
                  {:interactions [packed-int]
                   :citations (set papers)
                   :type typ
                   :effector packed-effector
                   :affected packed-affected
                   :direction direction
                   :phenotype phenotype
                   :nearby 0})))
    data))

(defn obj-interactions [obj nearby?]
  (let [ints (gene-interactions obj nearby?)]
    (->> ints
         (mapcat (fn [interaction]
                   (map vector
                        (repeat interaction)
                        (interaction-info interaction obj nearby?))))
         (reduce (partial obj-interaction obj) {}))))

(defn interactions [gene]
  {:description "genetic and predicted interactions"
   :data
   {:edges (vals (:edges (obj-interactions gene false)))}})

(defn interaction-details [gene id]
  {:data (let [{:keys [types edges ntypes nodes phenotypes]}
               (obj-interactions gene true)]
           {:types types
            :edges (vals edges)
            :ntypes ntypes
            :nodes nodes
            :phenotypes phenotypes
            :showall 1})})

(def widget
  {:name generic/name-field
   :interactions interactions})
