(ns datomic-rest-api.rest.gene.widgets.mapping-data
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [datomic-rest-api.formatters.date :as dates]
   [datomic-rest-api.formatters.object :refer [pack-obj]]
   [datomic-rest-api.rest.gene.generic :as generic]))

(def q-twopt-mapping-posneg
  '[:find [?pn ...]
    :in $ ?gene
    :where
    (or-join [?gene ?pn]
             (and [?png1 :pos-neg-data.gene-1/gene ?gene]
                  [?pn :pos-neg-data/gene-1 ?png1])
             (and [?png2 :pos-neg-data.gene-2/gene ?gene]
                  [?pn :pos-neg-data/gene-2 ?png2]))])

(def multi-counts-gene-rule
  '[[(mc-obj ?mc ?gene) [?mcg :multi-counts.gene/gene ?gene]
     [?mc :multi-counts/gene ?mcg]]
    [(mc-obj ?mc ?gene) (mc-obj ?mc2 ?gene)
     [?mc :multi-counts/gene ?mc2]]])

(defn- twopt-item [tp]
  {:mapper (pack-obj "author" (first (:two-point-data/mapper tp)))
   :date (if (:two-point-data/date tp)
           (dates/format-date (:two-point-data/date tp)))
   :raw_data (:two-point-data/results tp)
   :genotype (:two-point-data/genotype tp)
   :comment (let [p2-remark (:two-point-data/remark tp)
                  remarks (map :two-point-data.remark/text p2-remark)
                  comment (str/join "<br>" remarks)]
              (if (empty? comment) "" comment ))
   :distance (format "%s (%s-%s)"
                     (or (:two-point-data/calc-distance tp) "0.0")
                     (or (:two-point-data/calc-lower-conf tp) "0")
                     (or (:two-point-data/calc-upper-conf tp) "0"))
   :point_1 (let [p1 (:two-point-data/gene-1 tp)
                  gp1 (:two-point-data.gene-1/gene p1)
                  vp1 (:two-point-data.gene-1/variation p1)]
              (remove nil?
                      [(pack-obj "gene" gp1)
                       (pack-obj "variation" vp1)]))
   :point_2 (let [p2 (:two-point-data/gene-2 tp)
                  gp2 (:two-point-data.gene-2/gene p2)
                  vp2 (:two-point-data.gene-2/variation p2)]
              (remove nil? [(pack-obj "gene" gp2)
                            (pack-obj "variation" vp2)]))})

(defn gene-mapping-twopt [gene]
  {:data
   (let [db (d/entity-db gene)
         id (:db/id gene)]
     (->> (d/q '[:find [?tp ...]
                 :in $ ?gene
                 :where
                 (or-join [?gene ?tp]
                          (and
                           [?tpg1 :two-point-data.gene-1/gene ?gene]
                           [?tp :two-point-data/gene-1 ?tpg1])
                          (and
                           [?tpg2 :two-point-data.gene-2/gene ?gene]
                           [?tp :two-point-data/gene-2 ?tpg2]))]
               db id)
          (map (partial d/entity db))
          (map twopt-item)
          (seq)))
   :description "Two point mapping data for this gene"})

(defn pos-neg-item [pn]
  (let [pos-neg-gene (comp :pos-neg-data.gene-1/gene
                           :pos-neg-data/gene-1)
        pos-neg-gene2 (comp :pos-neg-data.gene-2/gene
                            :pos-neg-data/gene-2)
        pos-neg-loci (comp :pos-neg-data.locus-1/locus
                           :pos-neg-data/locus-1)
        pos-neg-loci2 (comp :pos-neg-data.locus-2/locus
                            :pos-neg-data/locus-2)
        items (->> [(pack-obj ((some-fn pos-neg-gene
                                        pos-neg-loci
                                        :pos-neg-data/allele-1
                                        :pos-neg-data/clone-1
                                        :pos-neg-data/rearrangement-1)
                               pn))
                    (pack-obj ((some-fn pos-neg-gene2
                                        pos-neg-loci2
                                        :pos-neg-data/allele-2
                                        :pos-neg-data/clone-2
                                        :pos-neg-data/rearrangement-2)
                               pn))]
                   (map (juxt :label identity))
                   (into {}))
        result (when-let [pn-results (:pos-neg-data/results pn)]
                 (str/split pn-results #"\s+"))]
    {:mapper (when-first [m (seq (:pos-neg-data/mapper pn))]
               (pack-obj "author" m))
     :comment (let [comment (str/join "<br>"
                                      (map :pos-neg-data.remark/text
                                           (:pos-neg-data/remark pn)))]
                (if (empty? comment) "" comment ))
     :date (when-let [pn-date (:pos-neg-data/date pn)]
             (dates/format-date pn-date))
     :result (if (seq result)
               (map #(or (items (str/replace % #"\." ""))
                         (str % " "))
                    result))}))

(defn gene-mapping-posneg [gene]
  {:data (let [db (d/entity-db gene)
               id (:db/id gene)]
           (->> (d/q q-twopt-mapping-posneg db id)
                (map (partial d/entity db))
                (map pos-neg-item)
                (seq)))
   :description "Positive/Negative mapping data for this gene"})

(defn- calc-mp-result [mp]
  (let [res (loop [node (:multi-pt-data/combined mp)
                   res  []]
              (cond
                (:multi-counts/gene node)
                (let [obj (:multi-counts/gene node)]
                  (recur obj (conj res
                                   [(:multi-counts.gene/gene obj)
                                    (:multi-counts.gene/int obj)])))
                :default
                res))
        tot (->> (map second res)
                 (filter identity)
                 (reduce +))
        sum (atom 0)
        open-paren (atom 0)]
    (->>
     (mapcat
      (fn [[obj count]]
        [(if (and (= @open-paren 0) (= count 0) (< @sum tot))
           (do
             (swap! open-paren inc)
             "("))
         (pack-obj obj)
         (if (and (not (= count 0)) (= @open-paren 1))
           (do
             (reset! open-paren 0)
             ")"))
         (if (and count (not (= count 0)))
           (do
             (swap! sum (fn[n] (+ n count)))
             (str " (" count "/" tot ") ")))])
      res)
     (filter identity))))

(defn- multipt-item [mp]
  {:comment (let [comment (->> mp
                               :multi-pt-data/remark
                               first
                               :multi-pt-data.remark/text)]
              (if (empty? comment) "" comment))
   :mapper (pack-obj "author" (first (:multi-pt-data/mapper mp)))
   :date (if-let [mp-date (:multi-pt-data/date mp)]
           (dates/format-date3 (str mp-date))
           "")
   :genotype (:multi-pt-data/genotype mp)
   :result (calc-mp-result mp)})

(defn gene-mapping-multipt [gene]
  {:data (let [db (d/entity-db gene)
               id (:db/id gene)]
           (->> (d/q '[:find [?mp ...]
                       :in $ % ?gene
                       :where (mc-obj ?mc ?gene)
                       (or
                        [?mp :multi-pt-data/a-non-b ?mc]
                        [?mp :multi-pt-data/b-non-a ?mc]
                        [?mp :multi-pt-data/combined ?mc])]
                     db multi-counts-gene-rule id)
                (map (partial d/entity db))
                (map multipt-item)
                (seq)))
   :description "Multi point mapping data for this gene"})

(def widget
  {:name generic/name-field
   :two_pt_data gene-mapping-twopt
   :pos_neg_data gene-mapping-posneg
   :multi_pt_data gene-mapping-multipt})
