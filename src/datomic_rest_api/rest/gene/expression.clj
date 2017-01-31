(ns datomic-rest-api.rest.gene.expression
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [datomic-rest-api.formatters.object :as obj]))

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

(defn fpkm-expression-summary-ls [gene]
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
    {:data (if (empty? results)
             nil
             {:controls controls
              :by_study studies
              :table {:fpkm {:data results}}})
     :description (str "Fragments Per Kilobase of transcript per "
                       "Million mapped reads (FPKM) expression data")}))
