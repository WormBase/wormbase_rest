(ns rest-api.classes.gene.expression
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.picture.core :as picture-fns]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- control-analysis? [analysis]
  (if-let [matched (re-matches #".+control_(mean|median)"
                               (:analysis/id analysis))]
    (let [[_ stat-type] matched]
      stat-type)))

(defn- fpkm-analysis-stage-result
  [[analysis fpkm stage]]
  {:value fpkm
   :life_stage (obj/pack-obj stage)
   :project_info (-> (first (:analysis/project analysis))
                     (obj/pack-obj)
                     (into {:experiment (-> (:analysis/id analysis)
                                            (str/split #"\.")
                                            (last))}))
   :label (obj/pack-obj analysis)})

(defn- fpkm-analysis-stage-control
  [[analysis fpkm stage]]
  (let [stat-type (->> (control-analysis? analysis)
                       (str "control ")
                       (keyword))]
    {stat-type {:text fpkm
                :evidence {:comment (:analysis/description analysis)}}
     :life_stage (if (re-find #"total_over_all_stages"
                              (:analysis/id analysis))
                   ;; refer to WormBase/website#4540
                   (obj/pack-text "total_over_all_stages")
                   (obj/pack-obj stage))}))

(defn- fpkm-analysis-stage-study
  [project]
  (let [idpv (:analysis/independent-variable project)]
    {(keyword (:analysis/id project))
     {:title (first (:analysis/title project))
      :tag (obj/pack-obj project)
      :indep_variable (map obj/humanize-ident idpv)
      :description (:analysis/description project)}}))

(def q-corresponding-transcript
  '[:find ?gene .
    :in $ ?transcript
    :where
    [?gene :gene/corresponding-transcript ?th]
    [?th :gene.corresponding-transcript/transcript ?transcript]])

(defn- process-fpkm
  [db rels]
  (let [result-tuples (map (fn [[analysis-id fpkm stage-id]]
                             (let [analysis (d/entity db analysis-id)
                                   stage (d/entity db stage-id)]
                               [analysis fpkm stage]))
                           rels)
        results (->> result-tuples
                     (filter (fn [[analysis]]
                               (not (control-analysis? analysis))))
                     (map fpkm-analysis-stage-result))
        controls (->> result-tuples
                      (filter (fn [[analysis]]
                                (control-analysis? analysis)))
                      (map fpkm-analysis-stage-control)
                      (group-by (fn [control]
                                  (:id (:life_stage control))))
                      (map (fn [[_ controls]]
                             (apply merge controls))))
        studies (->> result-tuples
                     (filter (fn [[analysis]]
                               (not (control-analysis? analysis))))
                     (map (fn [[analysis]]
                            (first (:analysis/project analysis))))
                     (set)
                     (map fpkm-analysis-stage-study)
                     (apply merge))]
    {:data (if (not (empty? results))
             {:controls controls
              :by_study studies
              :table {:fpkm {:data results}}})
     :description (str "Fragments Per Kilobase of transcript per "
                       "Million mapped reads (FPKM) expression data")}))

(defmulti fpkm-expression-summary-ls
  "Used for the expression widget."
  (fn [entity]
    (first (filter (fn [kw]
                     (kw entity))
                   [:gene/id :transcript/id :cds/id :pseudogene/id]))))

(defmethod fpkm-expression-summary-ls
  :gene/id
  [gene]
  (let [db (d/entity-db gene)]
    (->> (d/q '[:find ?analysis ?fpkm ?stage
                :in $ ?gene
                :where
                [?gene :gene/rnaseq ?rnaseq]
                [?rnaseq :gene.rnaseq/stage ?stage]
                [?rnaseq :gene.rnaseq/fpkm ?fpkm]
                [?rnaseq
                 :evidence/from-analysis
                 ?analysis]]
              db (:db/id gene))
         (process-fpkm db))))

(defmethod fpkm-expression-summary-ls
  :transcript/id
  [transcript]
  (let [db (d/entity-db transcript)]
    (->> (d/q '[:find ?analysis ?fpkm ?stage
                :in $ ?transcript
                :where
                [?transcript :transcript/rnaseq ?rnaseq]
                [?rnaseq :transcript.rnaseq/stage ?stage]
                [?rnaseq :transcript.rnaseq/fpkm ?fpkm]
                [?rnaseq
                 :evidence/from-analysis
                 ?analysis]]
              db (:db/id transcript))
         (process-fpkm db))))

(defmethod fpkm-expression-summary-ls
  :cds/id
  [cds]
  (let [db (d/entity-db cds)]
    (->> (d/q '[:find ?analysis ?fpkm ?stage
                :in $ ?cds
                :where
                [?cds :cds/rnaseq ?rnaseq]
                [?rnaseq :cds.rnaseq/stage ?stage]
                [?rnaseq :cds.rnaseq/fpkm ?fpkm]
                [?rnaseq
                 :evidence/from-analysis
                 ?analysis]]
              db (:db/id cds))
         (process-fpkm db))))

(defmethod fpkm-expression-summary-ls
  :pseudogene/id
  [pseudogene]
  (let [db (d/entity-db pseudogene)]
    (->> (d/q '[:find ?analysis ?fpkm ?stage
                :in $ ?pseudogene
                :where
                [?pseudogene :pseudogene/rnaseq ?rnaseq]
                [?rnaseq :pseudogene.rnaseq/stage ?stage]
                [?rnaseq :pseudogene.rnaseq/fpkm ?fpkm]
                [?rnaseq
                 :evidence/from-analysis
                 ?analysis]]
              db (:db/id pseudogene))
         (process-fpkm db))))


;;
;; Expression pattern related tables
;;

(defn- expr-pattern-type [expr-pattern]
  (let [type-keys
        [:expr-pattern/antibody
         :expr-pattern/cis-regulatory-element
         :expr-pattern/epic
         :expr-pattern/genome-editing
         :expr-pattern/in-situ
         :expr-pattern/localizome
         :expr-pattern/microarray
         :expr-pattern/northern
         :expr-pattern/reporter-gene
         :expr-pattern/rnaseq
         :expr-pattern/rt-pcr
         :expr-pattern/tiling-array
         :expr-pattern/western]]
    (->> (filter #(% expr-pattern) type-keys)
         (map obj/humanize-ident))))

(defn- short-term-list [expr-pattern terms]
  (let [capacity 5
        size (count terms)]
    (if (> size capacity)
      (concat (map pack-obj (take capacity terms))
              [(assoc (pack-obj expr-pattern)
                      :label (format "<strong>and %s more</strong>" (- size capacity)))])
      (map pack-obj terms))))

(defmulti expression-detail (fn [entity holder] (obj/obj-class entity)))

(defmethod expression-detail "expr-pattern" [expr-pattern qualifier]
  (pace-utils/vmap
   :Type (seq (expr-pattern-type expr-pattern))
   :Description (:expr-pattern/pattern expr-pattern)

   :Reagents
   (->> [:expr-pattern/transgene :expr-pattern/construct :expr-pattern/antibody-info :expr-pattern/variation]
        (map (fn [kw] (kw expr-pattern)))
        (apply concat
               (some->> (:expr-pattern/strain expr-pattern)
                        (conj []))
               (->> (:variation.expr-pattern/_expr-pattern expr-pattern)
                    (map :variation/_expr-pattern)))

        (map (fn [obj]
               (let [packed (pack-obj obj)]
                 (assoc packed
                        :label
                        (format "%s (%s)" (:label packed) (:class packed)))
                 )))
        (seq))

   :Paper (if-let [paper-holders (:expr-pattern/reference expr-pattern)]
            (->> paper-holders
                 (map :expr-pattern.reference/paper)
                 (map pack-obj)))
   :Expressed_in (->> (:qualifier/anatomy-term qualifier)
                      (short-term-list expr-pattern)
                      (seq))
   :Expressed_during (->> (:qualifier/life-stage qualifier)
                          (short-term-list expr-pattern)
                          (seq))

   ))

(defmethod expression-detail "expression-cluster" [expression-cluster expression-cluster-info]
  (->>
   [:expression-cluster/microarray-experiment
    :expression-cluster/mass-spectrometry
    :expression-cluster/rnaseq
    :expression-cluster/tiling-array
    :expression-cluster/qpcr
    :expression-cluster/expr-pattern]
   (reduce (fn [result key]
             (if-let [packed-values (some->> (get expression-cluster key)
                                             (map pack-obj))]
               (assoc result (case key
                               :expression-cluster/qpcr "qPCR"
                               :expression-cluster/rnaseq "RNASeq"
                               (obj/humanize-ident key))
                      packed-values)
               result))
           {})
   (into
    (pace-utils/vmap
     :Description
     (->> expression-cluster
          :expression-cluster/description
          (str/join " "))

     :Citation
     (->> expression-cluster
          :expression-cluster/reference
          (map :expression-cluster.reference/paper)
          (map pack-obj))

     :Algorithm
     (->> expression-cluster
          :expression-cluster/algorithm
          (str/join " "))

     :Method_of_isolation
     (->> expression-cluster-info
          (keys)
          (reduce (fn [result key]
                    (if (= "expression-cluster-info" (namespace key))
                      (if (get expression-cluster-info key)
                        (conj (case key
                                :expression-cluster-info/facs "FACS"
                                :expression-cluster-info/mrna-tagging "mRNA tagging"
                                (name key)))
                        result)
                      result))
                  {})
          )
     ))
   ))

(defn- expression-table-row [db ontology-term-dbid relations]
  (let [ontology-term (d/entity db ontology-term-dbid)]
    {:ontology_term (pack-obj ontology-term)

     :images
     (if-let [packed-images (->> relations
                                 (map (fn [[_ expr-pattern-dbid _]]
                                        (:picture/_expr-pattern (d/entity db expr-pattern-dbid))))
                                 (apply clojure.set/union)
                                 (filter (fn [picture]
                                           (or ((set (:picture/life-stage picture)) ontology-term)
                                               ((set (:picture/cellular-component picture)) ontology-term)
                                               ((set (:picture/anatomy picture)) ontology-term))))
                                 (map picture-fns/pack-image)
                                 (seq))]
       {:curated_images packed-images})

     :details
     (map (fn [[_ expr-pattern-dbid qualifier-dbid]]
            (let [expr-pattern (d/entity db expr-pattern-dbid)
                  qualifier (d/entity db qualifier-dbid)]
              {:text (pack-obj expr-pattern)
               :evidence (expression-detail expr-pattern qualifier)
               }))
          relations)}))

(defn- expression-table [db ontology-relations]
  ;; ontology-relation is a collection of tuples [ontology-term-dbid expr-pattern-dbid qualifier-dbid]
  (->> ontology-relations
       (group-by first)
       (map (fn [[term-dbid subset-relstions]]
              (expression-table-row db term-dbid subset-relstions)))
       (seq)))

(defn expressed-in [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [anatomy-relations
           (d/q '[:find ?t ?ep ?th
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  (not [?ep :expr-pattern/epic])
                  [?ep :expr-pattern/anatomy-term ?th]
                  [?th :expr-pattern.anatomy-term/anatomy-term ?t]]
                db (:db/id gene))
           anatomy-relations-from-cluster
           (d/q '[:find ?t ?ec ?ah
                  :in $ ?gene
                  :where
                  [?gh :expression-cluster.gene/gene ?gene]
                  [?ec :expression-cluster/gene ?gh]
                  [?ec :expression-cluster/anatomy-term ?ah]
                  [?ah :expression-cluster-info/enriched ?]
                  [?ah :expression-cluster.anatomy-term/anatomy-term ?t]]
                db (:db/id gene))]
       (expression-table db (concat anatomy-relations
                                    anatomy-relations-from-cluster)))
     :description "the tissue in which the gene is expressed"}))

(defn expressed-during [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [life-stage-relations
           (d/q '[:find ?t ?ep ?th
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  (not [?ep :expr-pattern/epic])
                  [?ep :expr-pattern/life-stage ?th]
                  [?th :expr-pattern.life-stage/life-stage ?t]]
                db (:db/id gene))
           ;; leave out life-stage from expression cluster until SObA includes them
           ;; life-stage-relations-from-cluster
           ;; (d/q '[:find ?t ?ec
           ;;        :in $ ?gene
           ;;        :where
           ;;        [?gh :expression-cluster.gene/gene ?gene]
           ;;        [?ec :expression-cluster/gene ?gh]
           ;;        [?ec :expression-cluster/life-stage ?t]]
           ;;      db (:db/id gene))
           ]
       (expression-table db life-stage-relations))
     :description "the developmental stage in which the gene is expressed"}))

(defn subcellular-localization [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [go-term-relations
           (d/q '[:find ?t ?ep ?th
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  (not [?ep :expr-pattern/epic])
                  [?ep :expr-pattern/go-term ?th]
                  [?th :expr-pattern.go-term/go-term ?t]]
                db (:db/id gene))]
       (expression-table db go-term-relations))
     :description "the cellular component in which the gene is expressed"}))

;;
;; End of expression pattern related tables
;;


;;
;; Expression profile graph work
;;
(defn- expr-pattern-db [expr-pattern]
  (->> (:expr-pattern/db-info expr-pattern)
       (map #(assoc {}
                    :id (:expr-pattern.db-info/accession %)
                    :label (:database/name (:expr-pattern.db-info/database %))
                    :class (:database/id (:expr-pattern.db-info/database %))))))

(defn- expr-pattern-description [expr-pattern]
  (let [descriptions
        (some seq [(:expr-pattern/pattern expr-pattern)
                   (:expr-pattern/subcellular-localization expr-pattern)
                   (map :expr-pattern.remark/text (:expr-pattern/remark expr-pattern))])

        references
        (->> (:expr-pattern/reference expr-pattern)
             (map :expr-pattern.reference/paper)
             (map pack-obj))]
    (if references
      (let [text (str/join " " descriptions)
            max-word-count 50
            truncated-text (->> (str/split text #"\s+" (+ 1 max-word-count))
                                (take max-word-count)
                                (str/join " ")
                                (format "%s..."))]
        {:text truncated-text
         :evidence {:Reference references}}))))

(defn- profiling-graph-table-row [db expr-pattern-dbid]
  (let [expr-pattern (d/entity db expr-pattern-dbid)
        packed-images (->> (:picture/_expr-pattern expr-pattern)
                           (map picture-fns/pack-image)
                           (seq))]
    {:expression_pattern (assoc (pack-obj expr-pattern)
                                :curated_images packed-images)
     :description (expr-pattern-description expr-pattern)
     :type (seq (expr-pattern-type expr-pattern))
     :database (seq (expr-pattern-db expr-pattern))}))

(defn expression-profiling-graphs [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [expr-patterns
           (d/q '[:find [?ep ...]
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  (or [?ep :expr-pattern/microarray]
                      [?ep :expr-pattern/rnaseq]
                      [?ep :expr-pattern/tiling-array])]
                db (:db/id gene))]
       (seq (map #(profiling-graph-table-row db %) expr-patterns)))
     :description (str "expression profiles associated with the " (:gene/id gene))}))
;;
;; End of Expression profile graph work
;;

;; EPIC expression pattern, such as Expr10220 and Expr10221
(defn- epic-table-row [db expr-pattern-dbid]
  (let [expr-pattern (d/entity db expr-pattern-dbid)]
    {:expression_pattern
     (if-let [packed-images (->> (:picture/_expr-pattern expr-pattern)
                                 (map picture-fns/pack-image)
                                 (seq))]
       (assoc (pack-obj expr-pattern) :curated_images packed-images)
       (pack-obj expr-pattern))

     :type (seq (expr-pattern-type expr-pattern))
     :description (expr-pattern-description expr-pattern)
     :life_stage (->> (:expr-pattern/life-stage expr-pattern)
                      (map :expr-pattern.life-stage/life-stage)
                      (map pack-obj)
                      (seq))
     :expressed_in (->> (:expr-pattern/anatomy-term expr-pattern)
                        (map :expr-pattern.anatomy-term/anatomy-term)
                        (map pack-obj)
                        (seq))
     :go_term (->> (:expr-pattern/go-term expr-pattern)
                   (map :expr-pattern.go-term/go-term)
                   (map pack-obj)
                   (seq))
     :database (seq (expr-pattern-db expr-pattern))}))

(defn epic-expr-patterns [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [pattern-dbids
           (d/q '[:find [?ep ...]
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  [?ep :expr-pattern/epic]]
                db (:db/id gene))]
       (seq (map #(epic-table-row db %) pattern-dbids)))
     :description "Large-scale cellular resolution compendium of gene expression dynamics"}))


;; expression pattern images
(defn expr-pattern-images [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [picture-dbids
           (d/q '[:find [?pic ...]
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  [?pic :picture/expr-pattern ?ep]]
                db (:db/id gene))]
       (->> picture-dbids
            (map #(d/entity db %))
            (sort-by (fn [picture]
                       ;; is picture for expression profiling graph? If so, they come last
                       (->> picture
                            (:picture/expr-pattern)
                            (some (fn [expr-pattern]
                                    (or (:expr-pattern/microarray expr-pattern)
                                        (:expr-pattern/rnaseq expr-pattern)
                                        (:expr-pattern/tiling-array expr-pattern))))
                            (boolean))))
            (map picture-fns/pack-image)
            (seq)))
     :description "Expression pattern images"}))


;; example gene with expression movies WBGene00016948
(defn expression-movies [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [expr-pattern-dbids
           (d/q '[:find [?ep ...]
                  :in $ ?gene
                  :where
                  [?gh :expr-pattern.gene/gene ?gene]
                  [?ep :expr-pattern/gene ?gh]
                  [?ep :expr-pattern/movieurl ?th]]
                db (:db/id gene))]
       (if (not-empty expr-pattern-dbids)
         (reduce (fn [coll expr-pattern-dbid]
                   (let [expr-pattern (d/entity db expr-pattern-dbid)]
                     (assoc coll
                            (:expr-pattern/id expr-pattern)
                            {:object (pack-obj expr-pattern)
                             :movie (:expr-pattern/movieurl expr-pattern)
                             :details (str/join "; " (:expr-pattern/pattern expr-pattern))})))
                 {} expr-pattern-dbids)))
     :description "interactive 4D expression movies"}))


(defn expression-cluster [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [expr-clusters
           (d/q '[:find [?ec ...]
                  :in $ ?gene
                  :where
                  [?h :expression-cluster.gene/gene ?gene]
                  [?ec :expression-cluster/gene ?h]]
                db (:db/id gene))]
       (->> expr-clusters
            (map (partial d/entity db))
            (map #(assoc {}
                         :expression_cluster (pack-obj %)
                         :description (:expression-cluster/description %)))
            (seq)))
     :description "expression cluster data"}))


;;
;; anatomy functions field
;;

(defn- anatomy-functions-table-row [db [anatomy-function-dbid involved-dbid]]
  (let [anatomy-function (d/entity db anatomy-function-dbid)
        involved (d/entity db involved-dbid)]
    {:bp_inv {:text (pack-obj (:anatomy-function.involved/anatomy-term involved))
              :evidence (->> [:anatomy-function-info/autonomous
                              :anatomy-function-info/insufficient
                              :anatomy-function-info/necessary
                              :anatomy-function-info/nonautonomous
                              :anatomy-function-info/sufficient
                              :anatomy-function-info/unnecessary]
                             (reduce (fn [coll attr]
                                       (if-let [attr-values (attr involved)]
                                         (assoc coll
                                                (obj/humanize-ident attr)
                                                (format "%s" attr-values))
                                         coll))
                                     (pace-utils/vmap
                                      :remark (:anatomy-function-info/remark involved))))}
     :assay (if-let [assay-holders (seq (:anatomy-function/assay anatomy-function))]
              {:text (->> (map :anatomy-function.assay/ao-code assay-holders)
                          (map :ao-code/id)
                          (str/join "<br/>"))
               :evidence {:genotype
                          (->> (map :anatomy-function.assay/condition assay-holders)
                               (map :condition/genotype)
                               (reduce into [])
                               (str/join "<br/>"))}})
     :phenotype (pack-obj (:anatomy-function.phenotype/phenotype (:anatomy-function/phenotype anatomy-function)))
     :reference (pack-obj (:anatomy-function/reference anatomy-function))}))

(defn anatomy-functions [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [function-term-relations
           (d/q '[:find ?af ?at
                  :in $ ?gene
                  :where
                  [?h :anatomy-function.gene/gene ?gene]
                  [?af :anatomy-function/gene ?h]
                  [?af :anatomy-function/involved ?at]]
                db (:db/id gene))]
       (->> function-term-relations
            (map (partial anatomy-functions-table-row db))
            (seq)))
     :description "anatomy functions associatated with this gene"}))
;;
;; end of anatomy functions field
;;

;;
;; microarray topography map
;;

(defn- pack-expr-profile [expr-profile]
  (if-let [mountains (->> (:expr-profile/expr-map expr-profile)
                          (map :sk-map/mountain)
                          (seq))]
    (assoc (pack-obj expr-profile)
           :label
           (format "%s Mountain: %s" (:expr-profile/id expr-profile) (str/join " " mountains)))
    (pack-obj expr-profile)))

(def gene-pcr-product-rules
  '[[(gene-pcr-product ?gene ?pcr)
     [?gene :gene/corresponding-transcript ?th]
     [?th :gene.corresponding-transcript/transcript ?t]
     [?t :transcript/corresponding-pcr-product ?pcr]]
    [(gene-pcr-product ?gene ?pcr)
     [?gene :gene/corresponding-cds ?th]
     [?th :gene.corresponding-cds/cds ?t]
     [?t :cds/corresponding-pcr-product ?pcr]]])

(defn microarray-topology-map-position [gene]
  (let [db (d/entity-db gene)]
    {:data
     (let [expr-profile-dbids
           (d/q '[:find [?pr ...]
                  :in $ % ?gene
                  :where
                  (gene-pcr-product ?gene ?pcr)
                  [?pr :locatable/parent ?pcr]
                  [?pr :expr-profile/id]]
                db gene-pcr-product-rules (:db/id gene))]
       (->> expr-profile-dbids
            (map #(d/entity db %))
            (map pack-expr-profile)
            (sort-by :id)
            (seq)))
     :description "microarray topography map"}))

;;
;; End of microarray topography map
;;
