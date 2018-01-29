(ns rest-api.classes.clone.widgets.overview
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn canonical-for [clone] ; example C05B2
  {:data (when-let [cfhs (:clone/canonical-for clone)]
           (into {}
             (for [cfh cfhs
                   :let [cclone (:clone.canonical-for/clone cfh)]]
               {(:clone/id cclone) (pack-obj cclone)})))
   :description "clones that the requested clone is a canonical representative of"})

(defn maps [clone] ; example B0272 and C05B2
  {:data (when-let [chs (or
                          (:clone/map clone)
                          (:contig/map (:clone.pmap/contig (:clone/pmap clone))))]
           (into {}
             (for [h chs
                   :let [m (or (:clone.map/map h)
                               (:contig.map/map h))]]
               {(:map/id m) {:id (:map/id m)
                             :label (:map/id m)
                             :class "map"
                             :taxonomy "all"}})))
   :description "maps assigned to this clone"})

(defn sequence-status [clone]
  {:data (not-empty
           (merge
             (if (contains? clone :clone/finished)
               {:Finished (date/format-date6 (:clone/finished clone))})
             (if (contains? clone :clone/shotgun) ; found with DL2
               {:Shotgun nil})
             (when-let [accession (:clone/accession-number clone)]
               {:Accession_number accession})))
   :description "sequencing status of clone"})

(defn canonical-parent [clone] ; found in API/Object/Clone.pm line 191. needs access to fields that do not exist in datomic schema. 
  {:data nil
   :description "canonical parent for clone"})

(defn screened-negative [clone]
  {:data (when-let [phs (:clone/negative-gene clone)]
           (into {}
               (for [ph phs :let [gene (:clone.negative-gene/gene ph)]]
                 {(:gene/id gene) (merge
                                    {:weak nil}
                                    (pack-obj gene))})))
   :description "entities shown to NOT be contained within the requested clone"})

(defn url [clone]
  {:data (first (:clone/url clone))
   :description "The website for this clone"})

(defn remarks [clone]
  {:data (when-let [remarks (:clone/general-remark clone)]
           (for [remark remarks]
             {:text remark}))
   :description "Remarks"})

(defn lengths [clone]
  {:data (not-empty
           (pace-utils/vmap
             :Seq_length
             (:clone/seq-length clone)

             :Gel_length
             (:clone/gel-length clone)))
   :description "lengths relevant to this clone"})

(defn sequences [clone]; example AB070577
  {:data (when-let [sequences (:sequence/_clone clone)]
           (for [s sequences] (pack-obj s)))
   :description "sequences associated with this clone"})

(defn in-strain [clone]
  {:data (when-let [is (:clone/in-strain clone)]
            (pack-obj (first is)))
   :description "The current clone is found in this strain"})

(defn pcr-product [clone]
  {:data (when-let [pcrs (:pcr-product/_clone clone)]
           (let [pcr (first pcrs)]
             {:pcr_prodcut (pack-obj pcr)
              :oligo (when-let [ohs (:pcr-product/oligo pcr)]
                       (for [oh ohs
                             :let [oligo (:pcr-product.oligo/oligo oh)]]
                         {:obj (pack-obj oligo)
                          :sequence (:oligo/sequence oligo)}))}))
   :description "PCR product associated with this clone"})

(defn gridded-on [clone]
  {:data nil ; fields are missing for this to work. I beleive we need the field grid.
   :description "grid this clone was gridded on during fingerprinting"})

(defn type-field [clone]
  {:data (when-let [t (:clone/type clone)]
           (let [n (name (:clone.type/value t))]
             (case n
               "cdna"
               "cDNA"
               (str/capitalize n))))
   :description "The type of this clone"})

(defn screened-positive [clone]
  {:data (when-let [phs (:clone/positive-gene clone)]
             (into {}
               (for [ph phs :let [gene (:clone.positive-gene/gene ph)]]
                 {(:gene/id gene) (merge
                                    {:weak nil}
                                    (pack-obj gene))})))
   :description "entities shown to be contained within this clone"})

(defn expression-patterns [clone]
  {:data (when-let [ep (first (:expr-pattern/_clone clone))]
           (let [gene (:expr-pattern.gene/gene (first (:expr-pattern/gene ep)))]
          {:certainty (first (:qualifier/certain h))
           :expression_pattern {:taxonomy "all"
                                :class "expr_pattern"
                                :label (str "Expression pattern for "
                                            (if-let [n (or (:gene/public-name gene)
                                                           (:gene/id gene))]
                                              n
                                              "" ))
                                :id (:expr-pattern/id ep)}
           :reference (when-let [paper (first (:expr-pattern/reference ep))]
                        (:paper/id (:expr-pattern.reference/paper paper)))
           :gene (if (empty? gene) nil (pack-obj gene))
           :author (:author/id (last (:expr-pattern/author ep)))
           :description (first (:expr-pattern/pattern ep))}))
   :description (str "expression patterns associated with the Clone: " (:clone/id clone))})

(def widget
  {:canonical_for canonical-for
   :maps maps
   :sequence_status sequence-status
   :canonical_parent canonical-parent
   :screened_negative screened-negative
   :url url
   :remarks remarks
   :lengths lengths
   :sequences sequences
   :in_strain in-strain
   :pcr_product pcr-product
   :gridded_on gridded-on
   :name generic/name-field
   :taxonomy generic/taxonomy
   :genomic_position generic/genomic-position
   :type type-field
   :screened_positive screened-positive
   :expression_patterns expression-patterns})
