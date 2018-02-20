(ns rest-api.classes.gene.widgets.genetics
  (:require
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.classes.gene.variation :as variation]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

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
  {:data (some->> (:gene/reference-allele gene)
                  (map :gene.reference-allele/variation)
                  (map pack-obj))
   :description "the reference allele of the gene"})

(defn strains [gene]
  (let [strains (:gene/strain gene)]
    {:data (generic-functions/categorize-strains strains)
     :description "strains carrying this gene"}))

(def widget
  {:name             generic/name-field
   :alleles          variation/alleles
   :alleles_count    variation/alleles-count
   :rearrangements   rearrangements
   :reference_allele reference-allele
   :strains          strains})
