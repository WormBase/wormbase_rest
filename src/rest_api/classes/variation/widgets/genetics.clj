(ns rest-api.classes.variation.widgets.genetics
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.generic-functions :as generic-functions]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn gene-class [variation]
  {:data (some->> (:variation/gene-class variation)
                  (map pack-obj))
   :description "the class of the gene the variation falls in, if any"})

(defn corresponding-gene [variation]
  {:data (some->> (:variation/corresponding-transgene variation)
                  (map pack-obj))
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
                               alleles (d/q '[:find [?var ...]
                                              :in $ ?variation ?gene
                                              :where [?vh :variation.gene/gene ?gene]
                                              [?var :variation/gene ?vh]
                                              [?var :variation/allele true]
                                              (not [?var :variation/phenotype _])
                                              [(not= ?var ?variation)]]
                                            db (:db/id variation) (:db/id gene))
                               polymorphisms (filter
                                               (fn [aid]
                                                 (let [allele (d/entity db aid)]
                                                   (contains? allele :variation/confirmed-snp))) alleles)
                               sequenced-alleles (filter
                                                   (fn [aid]
                                                     (let [allele (d/entity db aid)]
                                                       (not
                                                         (contains? allele :variation/confirmed-snp)))) alleles)]
                           {:polymorphisms (map pack-obj (d/entity db polymorphisms))
                            :sequenced_alleles (some->> sequenced-alleles
                                                        (d/entity db)
                                                        (map pack-obj))}))))
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
  {:data nil ; looks like it is from phenotype-info
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
