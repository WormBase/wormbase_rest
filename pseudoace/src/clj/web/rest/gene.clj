(ns web.rest.gene
  (:use cheshire.core
        web.rest.object)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap]]))

(def q-gene-rnai-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?gid
    :where [?g :gene/id ?gid]
           [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype ?ph]
           [?ph :rnai.phenotype/phenotype ?pheno]])

(def q-gene-rnai-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?gid
    :where [?g :gene/id ?gid]
           [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype-not-observed ?ph]
    [?ph :rnai.phenotype-not-observed/phenotype ?pheno]])

(def q-gene-var-pheno
   '[:find ?pheno (distinct ?ph)
     :in $ ?gid
     :where [?g :gene/id ?gid]
            [?gh :variation.gene/gene ?g]
            [?var :variation/gene ?gh]
            [?var :variation/phenotype ?ph]
            [?ph :variation.phenotype/phenotype ?pheno]])
 
(def q-gene-var-not-pheno
   '[:find ?pheno (distinct ?ph)
     :in $ ?gid
     :where [?g :gene/id ?gid]
            [?gh :variation.gene/gene ?g]
            [?var :variation/gene ?gh]
            [?var :variation/phenotype-not-observed ?ph]
     [?ph :variation.phenotype-not-observed/phenotype ?pheno]])

(defn- author-lastname [author-holder]
  (or
   (->> (:affiliation/person author-holder)
        (first)
        (:person/last-name))
   (-> (:paper.author/author author-holder)
       (:author/id)
       (str/split #"\s+")
       (last))))

(defn- author-list [paper]
  (let [authors (->> (:paper/author paper)
                     (sort-by :ordered/index))]
    (cond
     (= (count authors) 1)
     (author-lastname (first authors))

     (< (count authors) 6)
     (let [names (map author-lastname authors)]
       (str (str/join ", " (butlast names)) " & " (last names)))

     :default
     (str (author-lastname (first authors)) " et al."))))


(defn- evidence-paper [paper]
  {:class "paper"
   :id (:paper/id paper)
   :taxonomy "all"
   :label (str (author-list paper) ", " (:paper/publication-date paper))})

(defn- phenotype-table [db id not?]
  (let [var-phenos (into {} (q (if not?
                                 q-gene-var-not-pheno
                                 q-gene-var-pheno)
                               db id))
        rnai-phenos (into {} (q (if not?
                                  q-gene-rnai-not-pheno
                                  q-gene-rnai-pheno)
                                db id))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (->>
     (for [pid phenos :let [pheno (entity db pid)]]
       [(:phenotype/id pheno)
        {:object
         {:class "phenotype"
          :id (:phenotype/id pheno)
          :label (:phenotype.primary-name/text (:phenotype/primary-name pheno))
          :taxonomy "all"}
         :evidence
         (vmap
          "Allele:"
          (if-let [vp (seq (var-phenos pid))]
            (for [v vp
                  :let [holder (entity db v)
                        var ((if not?
                               :variation/_phenotype-not-observed
                               :variation/_phenotype)
                             holder)]]
              {:text
               {:class "variation"
                :id (:variation/id var)
                :label (:variation/public-name var)
                :style (if (= (:variation/seqstatus var)
                              :variation.seqstatus/sequence)
                         "font-weight: bold"
                         0)
                :taxonomy "c_elegans"}
               :evidence
               (vmap
                :Person_evidence
                (seq
                 (for [person (:phenotype-info/person-evidence holder)]
                   {:class "person"
                    :id (:person/id person)
                    :label (:person/standard-name person)
                    :taxonomy "all"}))

                :Anatomy_term
                (seq
                 (for [anatomy (:phenotype-info/anatomy-term holder)]
                   (pack-obj "anatomy-term" (:phenotype-info.anatomy-term/anatomy-term anatomy))))

                :Curator_confirmed
                (seq
                 (for [person (:phenotype-info/curator-confirmed holder)]
                   {:class "person"
                    :id (:person/id person)
                    :label (:person/standard-name person)
                    :taxonomy "all"}))

                :Paper_evidence
                (seq
                 (for [paper (:phenotype-info/paper-evidence holder)]
                   (evidence-paper paper)))

                :Remark
                (seq
                 (map :phenotype-info.remark/text
                      (:phenotype-info/remark holder))))}))
          
          "RNAi:"
          (if-let [rp (seq (rnai-phenos pid))]
            (for [r rp
                  :let [holder (entity db r)
                          rnai ((if not?
                                  :rnai/_phenotype-not-observed
                                  :rnai/_phenotype)
                                holder)]]
              {:text
               {:class "rnai"
                :id (:rnai/id rnai)
                :label (:rnai/id rnai)
                :style 0
                :taxonomy "c_elegans"}
               :evidence
               (vmap
                :Genotype (:rnai/genotype rnai)
                :Remark (seq (map :phenotype-info.remark/text
                                  (:phenotype-info/remark holder)))
                :Strain (:strain/id (:rnai/strain rnai))
                :Paper (if-let [paper (:rnai/reference rnai)]
                         (evidence-paper paper)))})))}])
     (into {}))))
      

(defn- gene-phenotype [db id]
  (let [gids (q '[:find ?g :in $ ?gid :where [?g :gene/id ?gid]] db id)]
    (if-let [[gid] (first gids)]
      {:name id
       :class "gene"
       :uri "whatevs"
       :fields
       {
        :name
        {:data 
         {:id id
          :label (:gene/public-name (entity db gid))
          :class "gene"
          :taxonomy "c_elegans"}
         :description (format "The name and WormBase internal ID of %s" id)}

        :phenotype
        {:data
         {:Phenotype              (phenotype-table db id false)
          :Phenotype_not_observed (phenotype-table db id true)}
         :description
         "The phenotype summary of the gene"}}})))
         
        
(defn gene-phenotype-rest [db id]
  {:status 200
   :content-type "application/json"
   :body (generate-string (gene-phenotype db id) {:pretty false})})

;; Needs better support for non-gene things.

(defn gene-mapping-twopt
  [db id]
  (->> (q '[:find [?tp ...]
            :in $ ?gene
            :where (or-join [?gene ?tp]
                     (and [?tpg1 :two-point-data.gene-1/gene ?gene]
                          [?tp :two-point-data/gene-1 ?tpg1])
                     (and [?tpg2 :two-point-data.gene-2/gene ?gene]
                          [?tp :two-point-data/gene-2 ?tpg2]))]
          db id)
       (map (partial entity db))
       (map
        (fn [tp]
          {:mapper     (pack-obj "author" (first (:two-point-data/mapper tp)))
           :date       (:two-point-data/date tp)
           :raw_data   (:two-point-data/results tp)
           :genotype   (:two-point-data/genotype tp)
           :comment    (str/join "<br>" (map :two-point-data.remark/text (:two-point-data/remark tp)))
           :distance   (format "%s (%s-%s)" (or (:two-point-data/calc-distance tp) "0.0")
                                            (:two-point-data/calc-lower-conf tp)
                                            (:two-point-data/calc-upper-conf tp))
           :point_1    (let [p1 (:two-point-data/gene-1 tp)]
                         [(pack-obj "gene" (:two-point-data.gene-1/gene p1))
                          (pack-obj "variation" (:two-point-data.gene-1/variation p1))])
           :point_2    (let [p2 (:two-point-data/gene-2 tp)]
                         [(pack-obj "gene" (:two-point-data.gene-2/gene p2))
                          (pack-obj "variation" (:two-point-data.gene-2/variation p2))])}
          ))))

(defn gene-mapping-posneg
  [db id]
  (->> (q '[:find [?pn ...]
            :in $ ?gene
            :where (or-join [?gene ?pn]
                      (and [?png1 :pos-neg-data.gene-1/gene ?gene]
                           [?pn :pos-neg-data/gene-1 ?png1])
                      (and [?png2 :pos-neg-data.gene-2/gene ?gene]
                           [?pn :pos-neg-data/gene-2 ?png2]))]
          db id)
       (map (partial entity db))
       (map
        (fn [pn]
          (let [items (->> [(pack-obj ((some-fn (comp :pos-neg-data.gene-1/gene :pos-neg-data/gene-1)
                                                (comp :pos-neg-data.locus-1/locus :pos-neg-data/locus-1)
                                                :pos-neg-data/allele-1
                                                :pos-neg-data/clone-1
                                                :pos-neg-data/rearrangement-1)
                                       pn))
                            (pack-obj ((some-fn (comp :pos-neg-data.gene-2/gene :pos-neg-data/gene-2)
                                                (comp :pos-neg-data.locus-2/locus :pos-neg-data/locus-2)
                                                :pos-neg-data/allele-2
                                                :pos-neg-data/clone-2
                                                :pos-neg-data/rearrangement-2)
                                       pn))]
                           (map (juxt :label identity))
                           (into {}))
                result (str/split (:pos-neg-data/results pn) #"\s+")]
          {:mapper    (pack-obj "author" (first (:pos-neg-data/mapper pn)))
           :date      (:pos-neg-data/date pn)
           :result    (map #(or (items (str/replace % #"\." ""))
                                (str " " % " "))
                           result)})
           
          ))))
          
(defn gene-mapping-multipt
  [db id]
  (->> (q '[:find [?mp ...]
            :in $ % ?gene
            :where (mc-obj ?mc ?gene)
            (or
             [?mp :multi-pt-data/a-non-b ?mc]
             [?mp :multi-pt-data/b-non-a ?mc]
             [?mp :multi-pt-data/combined ?mc])]
          db
          '[[(mc-obj ?mc ?gene) [?mcg :multi-counts.gene/gene ?gene]
                                [?mc :multi-counts/gene ?mcg]]
            [(mc-obj ?mc ?gene) (mc-obj ?mc2 ?gene)
                                [?mc :multi-counts/gene ?mc2]]]
          id)
       (map (partial entity db))
       (map
        (fn [mp]
          {:comment  (->> mp
                          :multi-pt-data/remark
                          first
                          :multi-pt-data.remark/text)
           :mapper   (pack-obj "author" (first (:multi-pt-data/mapper mp)))
           :date     (:multi-pt-data/date mp)
           :genotype (:multi-pt-data/genotype mp)
           :result   (let [res (loop [node (first (:multi-pt-data/combined mp))
                                      res  []]
                                 (cond
                                  (:multi-counts/gene node)
                                  (let [obj (:multi-counts/gene node)]
                                    (recur obj (conj res [(:multi-counts.gene/gene obj)
                                                          (:multi-counts.gene/int obj)])))
                                  
                                  :default res))
                           tot (->> (map second res)
                                    (filter identity)
                                    (reduce +))]
                       (->>
                        (mapcat
                         (fn [[obj count]]
                           [(pack-obj obj)
                            (if count
                              (str " (" count "/" tot ") "))])
                         res)
                        (filter identity)))}
          

          ))))
                        
    

(defn gene-mapping-data
  "Retrieve a map representing a mapping_data API response"
  [db id]
  (if-let [gid (q '[:find ?g .
                    :in $ ?gid
                    :where [?g :gene/id ?gid]]
                  db id)]
    {:name id
     :class "gene"
     :uri "whatevs"
     :fields
     {:name
      {:data (pack-obj "gene" (entity db gid))
       :description (format "The name and Wormbase internal ID of %s" id)}

      :two_pt_data
      {:data (seq (gene-mapping-twopt db gid))
       :description "Two point mapping data for this gene"}

      :pos_neg_data
      {:data (seq (gene-mapping-posneg db gid))
       :description "Positive/Negative mapping data for this gene"}

      :multi_pt_data
      {:data (seq (gene-mapping-multipt db gid))
       :description "Multi point mapping data for this gene"}}}))
 

(defn gene-mapping-data-rest [db id]
  {:status 200
   :content-type "application/json"
   :body (generate-string (gene-mapping-data db id) {:pretty true})})
