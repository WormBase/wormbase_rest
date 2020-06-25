(ns rest-api.classes.gene.widgets.phenotype
  (:require
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.paper.core :as paper-core]
   [rest-api.classes.phenotype.core :as phenotype-core]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn parse-int
  "Format a string as a number."
  [s]
  (Integer. (re-find #"\d+" s)))

(def q-gene-rnai-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype ?ph]
           [?ph :rnai.phenotype/phenotype ?pheno]])

(def q-gene-rnai-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype-not-observed ?ph]
           [?ph :rnai.phenotype-not-observed/phenotype ?pheno]])

(def q-gene-construct-transgene-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where
    [?gh :phenotype-info.caused-by-gene/gene ?g]
    [?ph :phenotype-info/caused-by-gene ?gh]
    [?tg :transgene/phenotype ?ph]
    [?ph :transgene.phenotype/phenotype ?pheno]])

(def q-gene-var-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :variation.gene/gene ?g]
           [?var :variation/gene ?gh]
           [?var :variation/phenotype ?ph]
           [?ph :variation.phenotype/phenotype ?pheno]])

(def q-gene-cons-transgene
  '[:find ?tg (distinct ?tg)
    :in $ ?g
    :where [?cbg :construct.driven-by-gene/gene ?g]
           [?cons :construct/driven-by-gene ?cbg]
           [?cons :construct/transgene-construct ?tg]])

(def q-gene-cons-transgene-test
  '[:find ?cons (distinct ?cons)
    :in $ ?g
    :where [?cbg :construct.driven-by-gene/gene ?g]
           [?cons :construct/driven-by-gene ?cbg]])


(def q-gene-cons-transgene-phenotype
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?cbg :construct.driven-by-gene/gene ?g]
           [?cons :construct/driven-by-gene ?cbg]
           [?cons :construct/transgene-construct ?tg]
           [?tg :transgene/phenotype ?ph]
           [?ph :transgene.phenotype/phenotype ?pheno]])

(def q-gene-var-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?g
    :where [?gh :variation.gene/gene ?g]
           [?var :variation/gene ?gh]
           [?var :variation/phenotype-not-observed ?ph]
           [?ph :variation.phenotype-not-observed/phenotype ?pheno]])

(defn- get-pato-combinations-gene [db pid rnai-phenos var-phenos not?]
  (if-let [vp (distinct (concat (rnai-phenos pid) (var-phenos pid)))]
    (let [patos (for [v vp
                      :let [holder (d/entity db v)]]
                     (phenotype-core/get-pato-from-holder holder))]
      (apply merge patos))))

(defn- phenotype-table-entity-overexpressed
  [db pheno pato-key entity pid trans-phenos]
  {:entity entity
   :phenotype {:class "phenotype"
               :id (:phenotype/id pheno)
               :label (:phenotype.primary-name/text
                        (:phenotype/primary-name pheno))
               :taxonomy "all"}
   :evidence
     (if-let [tp (seq (trans-phenos pid))]
       (not-empty
         (remove nil?
                 (for [t tp
                       :let [holder (d/entity db t)
                             transgene (:transgene/_phenotype holder)
                             pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                             trans-pato-key (first pato-keys)]]
                   (if (= pato-key trans-pato-key)
                     {:text (pack-obj transgene)
                      :evidence (phenotype-core/get-evidence holder transgene pheno)})))))})

(defn- phenotype-table-entity
  [db pheno pato-key entity pid var-phenos rnai-phenos not-observed?]
  {:entity entity
   :phenotype {:class "phenotype"
               :id (:phenotype/id pheno)
               :label (:phenotype.primary-name/text
                       (:phenotype/primary-name pheno))
               :taxonomy "all"}
   :evidence
   (pace-utils/vmap
     :Allele
     (when-let [vp (seq (var-phenos pid))]
       (not-empty
         (remove nil?
                 (for [v vp
                       :let [holder (d/entity db v)
                             var ((if not-observed?
                                    :variation/_phenotype-not-observed
                                    :variation/_phenotype)
                                  holder)
                             pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                             var-pato-key (first pato-keys)]]
                   (if (= pato-key var-pato-key)
                     {:text
                      {:class "variation"
                       :id (:variation/id var)
                       :label (:variation/public-name var)
                       :style (if (= (:variation/seqstatus var)
                                     :variation.seqstatus/sequenced)
                                "font-weight:bold"
                                0)
                       :taxonomy "c_elegans"}
                      :evidence (phenotype-core/get-evidence holder var pheno)})))))
     :RNAi
     (when-let [rp (seq (rnai-phenos pid))]
       (not-empty
         (remove nil?
                 (for [r rp]
                   (let [holder (d/entity db r)
                         pato-keys (keys (phenotype-core/get-pato-from-holder holder))
                         rnai ((if not-observed?
                                 :rnai/_phenotype-not-observed
                                 :rnai/_phenotype) holder)
                         rnai-pato-key (first pato-keys)]
                     (if (= rnai-pato-key pato-key)
                       {:text
                        {:class "rnai"
                         :id (:rnai/id rnai)
                         :label (str (parse-int (:rnai/id rnai)))
                         :taxonomy "c_elegans"}
                        :evidence
                        (merge
                          (pace-utils/vmap
                           :Genotype
                           (:rnai/genotype rnai)

                           :Strain
                           (pack-obj (:rnai/strain rnai))

                           :paper
                           (let [paper-ref (:rnai/reference rnai)]
                             (if-let [paper (:rnai.reference/paper paper-ref)]
                               (paper-core/evidence paper))))
                          (phenotype-core/get-evidence holder rnai pheno))})))))))})

(defn- phenotype-table-overexpressed [db gene]
  (let [trans-phenos (into {} (d/q
                                q-gene-construct-transgene-pheno
                                db gene))]
    (->>
      (flatten
        (for [pid (keys trans-phenos)
              :let [pheno (d/entity db pid)]]
          (let [pcs (phenotype-core/get-pato-combinations
                      db
                      pid
                      trans-phenos)]
            (if (nil? pcs)
              (phenotype-table-entity-overexpressed
                db
                pheno
                nil
                nil
                pid
                trans-phenos)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity-overexpressed
                  db
                  pheno
                  pato-key
                  entity
                  pid
                  trans-phenos))))))
      (into []))))

(defn- phenotype-table [db gene not-observed?]
  (let [var-phenos (into {} (d/q (if not-observed?
                                   q-gene-var-not-pheno
                                   q-gene-var-pheno)
                                 db gene))
        rnai-phenos (into {} (d/q (if not-observed?
                                    q-gene-rnai-not-pheno
                                    q-gene-rnai-pheno)
                                  db gene))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (->> (flatten
          (for [pid phenos
                :let [pheno (d/entity db pid)]]
            (let [pcs (get-pato-combinations-gene
                        db
                        pid
                        rnai-phenos
                        var-phenos
                        not-observed?)]
            (if (nil? pcs)
              (phenotype-table-entity db
                                      pheno
                                      nil
                                      nil
                                      pid
                                      var-phenos
                                      rnai-phenos
                                      not-observed?)
              (for [[pato-key entity] pcs]
                (phenotype-table-entity db
                                        pheno
                                        pato-key
                                        entity
                                        pid
                                        var-phenos
                                        rnai-phenos
                                        not-observed?))))))
         (into []))))

(defn drives-overexpression [gene]
  (let [data (phenotype-table-overexpressed (d/entity-db gene) (:db/id gene))]
    {:data data
     :description (str "phenotypes due to overexpression under the promoter of this gene")}))

(defn phenotype-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) false)]
    {:data data
     :description "The Phenotype summary of the gene"}))

(defmulti phenotype-annotation-details
  (fn [holder allele-or-rnai pheno]
    (obj/obj-class allele-or-rnai)))

(defmethod phenotype-annotation-details "variation" [holder allele pheno]
  {:Allele
   {:text (pack-obj allele)
    :evidence (phenotype-core/get-evidence holder allele pheno)}})

(defmethod phenotype-annotation-details "transgene" [holder transgene pheno]
  {:Transgene
   {:text (pack-obj transgene)
    :evidence (phenotype-core/get-evidence holder transgene pheno)}})

(defmethod phenotype-annotation-details "rnai" [holder rnai pheno]
  {:RNAi
   {:text (pack-obj rnai)
    :evidence (merge
               (pace-utils/vmap :Genotype (:rnai/genotype rnai)

                                :Strain (pack-obj (:rnai/strain rnai))

                                :Paper_evidence
                                (let [paper-ref (:rnai/reference rnai)]
                                  (if-let [paper (:rnai.reference/paper paper-ref)]
                                    (paper-core/evidence paper))))
               (phenotype-core/get-evidence holder rnai pheno))}})

(defn- phenotype-field-flat-row [db pheno-dbid allele-or-rnai-dbid holder-dbid]
  (let [pheno (d/entity db pheno-dbid)
        allele-or-rnai (d/entity db allele-or-rnai-dbid)
        holder (d/entity db holder-dbid)]
    {:entity (phenotype-core/get-pato-from-holder holder)
     :phenotype (pack-obj pheno)
     :evidence (phenotype-annotation-details holder allele-or-rnai pheno)}))

(defn phenotype-field-flat [gene]
  (let [db (d/entity-db gene)
        pheno-var-results (d/q '[:find ?pheno ?var ?ph
                                 :in $ ?g
                                 :where [?gh :variation.gene/gene ?g]
                                 [?var :variation/gene ?gh]
                                 [?var :variation/phenotype ?ph]
                                 [?ph :variation.phenotype/phenotype ?pheno]]
                               db
                               (:db/id gene))
        pheno-rnai-results (d/q '[:find ?pheno ?rnai ?ph
                                  :in $ ?g
                                  :where [?gh :rnai.gene/gene ?g]
                                  [?rnai :rnai/gene ?gh]
                                  [?rnai :rnai/phenotype ?ph]
                                  [?ph :rnai.phenotype/phenotype ?pheno]]
                                db
                                (:db/id gene))]
    {:data (->> (concat pheno-var-results pheno-rnai-results)
                (map (partial apply phenotype-field-flat-row db))
                (seq))
     :description "The Phenotype summary of the gene"}))

(defn phenotype-not-observed-field-flat [gene]
  (let [db (d/entity-db gene)
        pheno-var-results (d/q '[:find ?pheno ?var ?ph
                                 :in $ ?g
                                 :where [?gh :variation.gene/gene ?g]
                                 [?var :variation/gene ?gh]
                                 [?var :variation/phenotype-not-observed ?ph]
                                 [?ph :variation.phenotype-not-observed/phenotype ?pheno]]
                               db
                               (:db/id gene))
        pheno-rnai-results (d/q '[:find ?pheno ?rnai ?ph
                                  :in $ ?g
                                  :where [?gh :rnai.gene/gene ?g]
                                  [?rnai :rnai/gene ?gh]
                                  [?rnai :rnai/phenotype-not-observed ?ph]
                                  [?ph :rnai.phenotype-not-observed/phenotype ?pheno]]
                                db
                                (:db/id gene))]
    {:data (->> (concat pheno-var-results pheno-rnai-results)
                (map (partial apply phenotype-field-flat-row db))
                (seq))
     :description "The Phenotype not observed summary of the gene"}))

(defn drives-overexpression-flat [gene]
  (let [db (d/entity-db gene)
        pheno-transgene-results (d/q '[:find ?pheno ?tg ?ph
                                       :in $ ?g
                                       :where
                                       [?gh :phenotype-info.caused-by-gene/gene ?g]
                                       [?ph :phenotype-info/caused-by-gene ?gh]
                                       [?tg :transgene/phenotype ?ph]
                                       [?ph :transgene.phenotype/phenotype ?pheno]]
                               db
                               (:db/id gene))]
    {:data (->> pheno-transgene-results
                (map (partial apply phenotype-field-flat-row db))
                (seq))
     :description (str "phenotypes due to overexpression under the promoter of this gene")}))


(defn phenotype-by-interaction [gene]
  (let [db (d/entity-db gene)
        gid (:db/id gene)
        table (d/q '[:find ?pheno (distinct ?int) ?int-type
                     :in $ ?gene
                     :where
                     [?ig
                      :interaction.interactor-overlapping-gene/gene
                      ?gene]
                     [?int :interaction/interactor-overlapping-gene ?ig]
                     [?int :interaction/interaction-phenotype ?pheno]
                     [?int :interaction/type ?type-id]
                     [?type-id :db/ident ?int-type]]
                   db gid)
        phenos (->> (map first table)
                    (set)
                    (map (fn [pid]
                           (let [pheno (d/entity db pid)]
                             [pid (obj/pack-obj "phenotype" pheno)])))
                    (into {}))
        inters (->> (mapcat second table)
                    (set)
                    (map
                     (fn [iid]
                       (let [int (d/entity db iid)]
                         [iid
                          {:interaction (obj/pack-obj "interaction" int)
                           :citations (map (partial obj/pack-obj "paper")
                                           (:interaction/paper int))}])))
                    (into {}))
        data (map (fn [[pheno pints int-type]]
                    {:interaction_type
                     (obj/humanize-ident int-type)
                     :phenotype
                     (phenos pheno)
                     :interactions
                     (map #(:interaction (inters %)) pints)
                     :citations
                     (map #(:citations (inters %)) pints)})
                  table)]
    {:data (if (empty? data) nil data)
     :description
     "phenotype based on interaction"}))

(defn phenotype-not-observed-field [gene]
  (let [data (phenotype-table (d/entity-db gene) (:db/id gene) true)]
    {:data data
     :description "The Phenotype not observed summary of the gene"}))

(def widget
  {:drives_overexpression    drives-overexpression
   :name                     generic/name-field
   :phenotype                phenotype-field
   :phenotype_by_interaction phenotype-by-interaction
   :phenotype_not_observed   phenotype-not-observed-field})
