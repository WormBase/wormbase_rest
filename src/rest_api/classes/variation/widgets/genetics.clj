(ns rest-api.classes.variation.widgets.genetics
  (:require
    [datomic.api :as d]
    [pseudoace.utils :as pace-utils]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn gene-class [variation]
  {:data (some->> (:variation/gene-class variation)
                  (first)
                  (pack-obj))
   :description "the class of the gene the variation falls in, if any"})

(defn corresponding-gene [variation]
  {:data (when-let [genes (some->> (:variation/gene variation)
                                   (map :variation.gene/gene)
                                   (map pack-obj))]
           [genes])
   :description "gene in which this variation is found (if any)"})

(defn reference-allele [variation]
  {:data (some->> (:gene.reference-allele/_variation variation)
                  (map :gene/_reference-allele)
                  (map pack-obj))
   :description "the gene that has this variation as a reference allele"})

(defn other-allele [variation]
  {:data (some->> (:variation/gene variation)
                  (map :variation.gene/gene)
                  (map (fn [gene]
                         (let [db  (d/entity-db gene)
                               alleles (some->> (d/q '[:find [?var ...]
                                                       :in $ ?variation ?gene
                                                       :where [?vh :variation.gene/gene ?gene]
                                                       [?var :variation/gene ?vh]
                                                       [?var :variation/allele true]
                                                       (not [?var :variation/phenotype _])
                                                       [(not= ?var ?variation)]]
                                                     db (:db/id variation) (:db/id gene))
                                                (map (fn [allele-id]
                                                       (d/entity db allele-id))))]
                           (not-empty
                             (pace-utils/vmap
                               :polymorphisms
                               (some->> alleles
                                        (filter
                                          (fn [allele]
                                            (contains? allele :variation/confirmed-snp)))
                                        (map pack-obj)
                                        (not-empty))

                               :sequenced_alleles
                               (some->> alleles
                                        (filter
                                          (fn [allele]
                                            (not (contains? allele :variation/confirmed-snp))))
                                        (map pack-obj)
                                        (not-empty)))))))
                  (remove nil?)
                  (not-empty))
   :description "other variations of the containing gene (if known)"})

(defn linked-to [variation]
  {:data (some->> (:variation/linked-to variation)
                  (map pack-obj))
   :description "linked_to"})

(defn strain [variation]
  {:data (some->> (:variation/strain variation)
                  (map :variation.strain/strain)
                  (generic-functions/categorize-strains))
   :description "strains carrying this variation"})

(defn rescued-by-transgene [variation]
  {:data (when-let [tg (:variation/corresponding-transgene variation)]
           (pack-obj tg))
   :description "transgenes that rescue phenotype(s) caused by this variation"})

(def widget
  {:name generic/name-field
   :gene_class gene-class
   :corresponding_gene corresponding-gene
   :reference_allele reference-allele
   :other_allele other-allele
   :linked_to linked-to
   :strain strain
   :rescued_by_transgene rescued-by-transgene})
