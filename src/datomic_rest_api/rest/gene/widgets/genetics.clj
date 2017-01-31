(ns datomic-rest-api.rest.gene.widgets.genetics
  (:require
   [datomic.api :as d]
   [datomic-rest-api.rest.gene.generic :as gene-fields]
   [datomic-rest-api.formatters.object :as obj :refer [pack-obj]]
   [datomic-rest-api.rest.gene.variation :as variation]
   [pseudoace.utils :as pace-utils]))

;; TODO: factor-out duplication

(defn rearrangements-positive [gene]
  (let [db (d/entity-db gene)]
    (->> (d/q '[:find [?ra ...]
                :in $ ?gene
                :where
                [?rag :rearrangement.gene-inside/gene ?gene]
                [?ra :rearrangement/gene-inside ?rag]]
              db (:db/id gene))
         (map #(pack-obj (d/entity db %))))))

(defn rearrangements-negative [gene]
   (let [db (d/entity-db gene)]
    (->> (d/q '[:find [?ra ...]
                :in $ ?gene
                :where
                [?rag :rearrangement.gene-outside/gene ?gene]
                [?ra :rearrangement/gene-outside ?rag]]
              db (:db/id gene))
         (map #(pack-obj (d/entity db %))))))

;; END-TODO: factor-out duplication

(defn rearrangements [gene]
  {:data (let [data {:positive (not-empty
                                (rearrangements-positive gene))
                     :negative (not-empty
                                (rearrangements-negative gene))}]
     (if (every? val data)
       data))
   :description "rearrangements involving this gene"})

(defn reference-allele [gene]
  {:data (let [data (->> (:gene/reference-allele gene)
                         (map :gene.reference-allele/variation)
                         (map (partial pack-obj "variation")))]
           (if (empty? data) nil data))
   :description "the reference allele of the gene"})

(defn- is-cgc? [strain]
  (some #(= (->> (:strain.location/laboratory %)
                 (:laboratory/id))
            "CGC")
        (:strain/location strain)))

(defn- strain-list [strains]
  (seq (map (fn [strain]
              (let [tgs (:transgene/_strain strain)]
                (pace-utils/vassoc
                 (pack-obj "strain" strain)
                 :genotype (:strain/genotype strain)
                 :transgenes (pack-obj "transgene" (first tgs)))))
            strains)))

;; TODO: factor-out duplication

(defn strains [gene]
  (let [strains (:gene/strain gene)]
    {:data
     (pace-utils/vmap
      :carrying_gene_alone_and_cgc
      (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                 (= (count (:gene/_strain %)) 1)
                                 (is-cgc? %))
                           strains))

      :carrying_gene_alone
      (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                 (= (count (:gene/_strain %)) 1)
                                 (not (is-cgc? %)))
                           strains))

      :available_from_cgc
      (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                     (not= (count (:gene/_strain %)) 1))
                                 (is-cgc? %))
                           strains))

      :others
      (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                     (not= (count (:gene/_strain %)) 1))
                                 (not (is-cgc? %)))
                           strains)))

     :description
     "strains carrying this gene"}))

;; END-TODO: factor-out duplication

(def widget
  {:name             gene-fields/name-field
   :alleles          variation/alleles
   :alleles_count    variation/alleles-count
   :rearrangements   rearrangements
   :reference_allele reference-allele
   :strains          strains})
