(ns rest-api.classes.gene.expression
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
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

(defmulti fpkm-expression-summary-ls
  "Used for the expression widget."
  (fn [entity]
    (contains? entity :gene/id)))

(defmethod fpkm-expression-summary-ls
  true
  [gene]
  (let [db (d/entity-db gene)
        result-tuples (->> (d/q '[:find ?analysis ?fpkm ?stage
                                  :in $ ?gene
                                  :where
                                  [?gene :gene/rnaseq ?rnaseq]
                                  [?rnaseq :gene.rnaseq/stage ?stage]
                                  [?rnaseq :gene.rnaseq/fpkm ?fpkm]
                                  [?rnaseq
                                   :evidence/from-analysis
                                   ?analysis]]
                                db (:db/id gene))
                           (map (fn [[analysis-id fpkm stage-id]]
                                  (let [analysis (d/entity db analysis-id)
                                        stage (d/entity db stage-id)]
                                    [analysis fpkm stage]))))
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

(defmethod fpkm-expression-summary-ls
  false
  [transcript]
  (let [db (d/entity-db transcript)]
    (->> (:db/id transcript)
         (d/q q-corresponding-transcript db)
         (d/entity db)
         (fpkm-expression-summary-ls))))



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

(defn- expr-pattern-detail [expr-pattern qualifier]
  (pace-utils/vmap
   :Type (seq (expr-pattern-type expr-pattern))
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

(defn- pack-image [picture]
  (let [prefix (if (re-find #"<Journal_URL>" (or (:picture/acknowledgement-template picture) ""))
                 (:paper/id (first (:picture/reference picture)))
                 (:person/id (first (:picture/contact picture))))
        [_ picture-name format-name] (re-matches #"(.+)\.(.+)" (:picture/name picture))]
    (-> picture
        (pack-obj)
        (assoc :thumbnail
               {:format (or format-name "")
                :name (str prefix "/" (or picture-name (:picture/name picture)))
                :class "/img-static/pictures"}))))

(defn- expression-table-row [db [ontology-term-dbid expr-pattern-dbid qualifier-dbid]]
  (let [ontology-term (d/entity db ontology-term-dbid)
        expr-pattern (d/entity db expr-pattern-dbid)
        qualifier (d/entity db qualifier-dbid)]
    {:ontology_term (pack-obj ontology-term)

     :expression_pattern
     (if-let [packed-images (->> (:picture/_expr-pattern expr-pattern)
                                 (map pack-image)
                                 (seq))]
       (assoc (pack-obj expr-pattern) :curated_images packed-images)
       (pack-obj expr-pattern))

     :details
     {:evidence (expr-pattern-detail expr-pattern qualifier)}}))

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
                db (:db/id gene))]
       (seq (map #(expression-table-row db %) anatomy-relations)))
     :description "the tissue that the gene is expressed in"}))

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
                db (:db/id gene))]
       (seq (map #(expression-table-row db %) life-stage-relations)))
     :description "the tissue that the gene is expressed in"}))

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
       (seq (map #(expression-table-row db %) go-term-relations)))
     :description "the tissue that the gene is expressed in"}))

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
      {:text (str/join "; " descriptions)
       :evidence {:Reference references}}
      (str/join "; " descriptions))))

(defn- profiling-graph-table-row [db expr-pattern-dbid]
  (let [expr-pattern (d/entity db expr-pattern-dbid)
        packed-images (->> (:picture/_expr-pattern expr-pattern)
                           (map pack-image)
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
                                 (map pack-image)
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
     :description "the tissue that the gene is expressed in"}))


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

(defn- anatomy-functions-table-row [db [anatomy-function-dbids involved-dbid]]
  (let [anatomy-function (d/entity db anatomy-function-dbids)
        involved (d/entity db involved-dbid)]
    {:bp_inv {:text (pack-obj (:anatomy-function.involved/anatomy-term involved))
              :evidence (->> [:anatomy-function-info/autonomous
                              :anatomy-function-info/insufficient
                              :anatomy-function-info/necessary
                              :anatomy-function-info/nonautonomous
                              :anatomy-function-info/remark
                              :anatomy-function-info/sufficient
                              :anatomy-function-info/unnecessary]
                             (reduce (fn [coll attr]
                                       (if-let [attr-values (attr involved)]
                                         (assoc coll
                                                (obj/humanize-ident attr)
                                                (str/join "<br/>" attr-values))
                                         coll))
                                     {}))}
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
