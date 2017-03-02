(ns rest-api.classes.gene.widgets.interactions
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.formatters.object :refer [obj-get obj-name pack-obj]]
   [rest-api.classes.generic :as generic]))

(def ^:private interactor-role-map
  {:interactor-info.interactor-type/effector :effector
   :interactor-info.interactor-type/affected :affected
   :interactor-info.interactor-type/trans-regulator :effector
   :interactor-info.interactor-type/cis-regulatory :effector
   :interactor-info.interactor-type/trans-regulated :affected
   :interactor-info.interactor-type/cis-regulated :affected})

(def ^:private interaction-targets
  [:interaction.feature-interactor/feature
   :interaction.interactor-overlapping-cds/cds
   :interaction.interactor-overlapping-gene/gene
   :interaction.interactor-overlapping-protein/protein
   :interaction.molecule-interactor/molecule
   :interaction.other-interactor/text
   :interaction.pcr-interactor/pcr-product
   :interaction.sequence-interactor/sequence
   :interaction.variation-interactor/variation])

;; TODO: check this list of targets
(def ^:private interaction-target
  (apply some-fn interaction-targets))

(def ^:private some-cds
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
     [(not= ?gene ?neighbour)]]

    ;; Not actually used since count trick is subtantially faster.
    [(gene-neighbour-interaction ?gene ?int)
     (gene-neighbour ?gene ?n1)
     (gene-interaction ?n1 ?int)
     [?int :interaction/interactor-overlapping-gene ?ih]
     [?ih :interaction.interactor-overlapping-gene/gene ?n2]
     [(not= ?n1 ?n2)]
     (gene-neighbour ?gene ?n2)]])

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

(defn- predicted [typ]
  (if (= typ "Predicted")
    1
    0))

(defn gene-interactions [obj nearby?]
  (let [db (d/entity-db obj)
        id (:db/id obj)]
    (map
     (partial d/entity db)
     (concat
      (gene-direct-interactions db id)
      (if nearby?
        (gene-nearby-interactions db id))))))

(defn- annotate-role [obj int-type role]
  (let [identify (keyword (:class role) "id")]
    (pace-utils/vassoc role
                       :predicted (predicted int-type)
                       :main (if (= (identify obj) (:id role))
                               1))))

(defn- assoc-interaction [obj typ data unpacked]
  (-> data
      (assoc-in [:nodes (:id unpacked)] (annotate-role obj typ unpacked))
      (assoc-in [:ntypes (:class unpacked)] 1)))

(defn- update-in-uniq [data path func value]
  ;; #dbg ^{:break/when (= (last path) :interactions)}
  (update-in data
             path
             (fn [old new]
               (vec (set (func old new))))
             value))

(defn- update-in-edges [data int-key packed-int papers]
  (-> data
      (update-in-uniq [:edges int-key :interactions]
                      conj
                      packed-int)
      (update-in-uniq [:edges int-key :citations]
                      into
                      papers)))

(defn- pack-int-roles [ref-obj int nearby? pred a b direction]
  (let [typ (interaction-type int)]
    (for [x a
          y b
          :let [xt (interaction-target x)
                yt (interaction-target y)]
          :when (and (pred xt yt)
                     (or nearby? (= xt ref-obj) (= yt ref-obj)))]
      (let [roles (zipmap [:effector :affected]
                          (sort-by (partial pred ref-obj) [xt yt]))]
        [(str (:label (pack-obj xt)) " " (:label (pack-obj yt)))
         (merge {:typ typ :direction direction} roles)]))))

(defn- interaction-info [int ref-obj nearby?]
  (let [int-type (:interaction/type int)
        not-has-int-type #(not (contains? int-type %))]
    (if (or (not-has-int-type :interaction.type/predicted)
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
                       (:interaction/feature-interactor int)
                       (:interaction/molecule-interactor int)))
            pack-iroles (partial pack-int-roles ref-obj int nearby?)]
        (->> (if (or effectors affected)
               (let [x (concat effectors others)
                     pred (constantly true)]
                 (pack-iroles pred x affected "Effector->Affected"))
               (pack-iroles #(not= %1 %2)
                            others
                            others
                            "non-directional"))
             (into {})
             (vals))))))

(defn- annotate-interactor-roles [obj typ int-roles]
  (->> int-roles
       (map pack-obj)
       (map (partial annotate-role obj typ))))

(defn- pack-papers [papers]
  (->> papers
       (map (partial pack-obj "paper"))
       #_(map (fn [paper]
              (update paper :label
                      (fn [label]
                        (str " " label)))))))

(defn- obj-interaction
  [obj nearby? data [interaction
                     {:keys [typ effector affected direction]}]]
  (if (every? not-empty (map some-interaction [effector affected]))
    (let [packed-roles (annotate-interactor-roles obj
                                                  typ
                                                  [effector affected])
          [packed-effector packed-affected] packed-roles
          int-pheno-key :interaction/interaction-phenotype
          [ename aname] (map :label packed-roles)
          ;; warning: could be more than one.

          ;; DEBUG: WBGene00000421
          ;; x (do
          ;;     (if (int-pheno-key interaction)
          ;;       (println "Got" (count (int-pheno-key interaction))
          ;;                "phenotypes for interaction:"
          ;;                (->> interaction
          ;;                     :interaction/interaction-phenotype
          ;;                     (map :phenotype/id))))
          ;;     1)
          phenotype (pack-obj "phenotype"
                              (first (int-pheno-key interaction)))
          mk-key #(str %1 " " %2 " " typ " " (:label phenotype))
          ;; interactions with the same endpoints but
          ;; a different phenotype get a separate row.
          e-key (mk-key ename aname)
          a-key (mk-key aname ename)
          i-label (str/join " : " (sort [ename aname]))
          packed-int (pack-obj "interaction" interaction :label i-label)
          papers (pack-papers (:interaction/paper interaction))
          assoc-int (partial assoc-interaction obj typ)
          result (-> data       
                     (assoc-in [:types typ] 1)
                     (assoc-int packed-effector)
                     (assoc-int packed-affected)
                     ;; (update-in [:phenotypes] phenotypes)
                                ;; pace-utils/vassoc
                                ;; (:id phenotype)
                                ;; phenotype)
          )]
      (cond
        (get-in result [:edges e-key])
        (update-in-edges result e-key packed-int papers)

        (get-in result [:edges a-key])
        (update-in-edges result a-key packed-int papers)

        :default
        (assoc-in result
                  [:edges e-key]
                  {:interactions [packed-int]
                   :citations (vec (set papers))
                   :type typ
                   :effector packed-effector
                   :affected packed-affected
                   :direction direction
                   :phenotype phenotype
                   :nearby (if nearby?
                             "1"
                             "0")})))
    data))

(defn obj-interactions [obj nearby?]
  (let [ints (gene-interactions obj nearby?)]
    (->> ints
         (mapcat (fn [interaction]
                   (map vector
                        (repeat interaction)
                        (interaction-info interaction obj nearby?))))
         (reduce (partial obj-interaction obj nearby?) {}))))

(defn interactions [gene]
  {:data (let [{:keys [types edges ntypes nodes phenotypes]}
               (obj-interactions gene false)
               vedges (vec (vals edges))]
           {:types types
            :edges vedges
            :edges_all vedges
            :ntypes ntypes
            :nodes nodes
            :phenotypes (->> vedges
                             (map :phenotype)
                             (filter identity)
                             (set)
                             (map (fn [pt]
                                    [(:id pt) pt]))
                             (into {})
                             (not-empty))
            :class "Gene"
            :showall "1"})
   :description "genetic and predicted interactions"})

(def widget
  {:name generic/name-field
   :interactions interactions})
