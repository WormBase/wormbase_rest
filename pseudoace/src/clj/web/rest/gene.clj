(ns web.rest.gene
  (:use cheshire.core)
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
   :body (generate-string (gene-phenotype db id) {:pretty true})})
