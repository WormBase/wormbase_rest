(ns rest-api.classes.gene.variation
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [rest-api.classes.variation.core :as variation-core]))

(defn- relevant-location [gene]
  (let [cds (->> (:gene/corresponding-cds gene)
                 (map :gene.corresponding-cds/cds))
        transcript (->> (:gene/transcript gene)
                        (map :gene.transcript/transcript))]
    (->> (concat cds transcript)
         (cons gene)
         (set))))

(defn alleles [gene]
  (let [db (d/entity-db gene)]
    {:data (->> (d/q '[:find [?var ...]
                       :in $ ?gene
                       :where
                       [?vh :variation.gene/gene ?gene]
                       [?var :variation/gene ?vh]
                       [?var :variation/phenotype _]]
                     db (:db/id gene))
                (map #(variation-core/process-variation (d/entity db %) (relevant-location gene))))
     :description
     "alleles and polymorphisms with associated phenotype"}))

(defn alleles-count [gene]
  (let [db (d/entity-db gene)]
    {:data
     (-> {}
         (assoc :polymorphisms
                (d/q '[:find (count ?var) .
                       :in $ ?gene
                       :where
                       [?vh :variation.gene/gene ?gene]
                       [?var :variation/gene ?vh]
                       (not [?var :variation/allele _])
                       (not [?var :variation/phenotype _])]
                     db (:db/id gene)))
         (assoc :alleles_other
                (d/q '[:find (count ?var) .
                       :in $ ?gene
                       :where
                       [?vh :variation.gene/gene ?gene]
                       [?var :variation/gene ?vh]
                       [?var :variation/allele _]
                       (not [?var :variation/phenotype _])]
                     db (:db/id gene))))
     :description "counts for alleles-other and polymorphisms"}))

(defn alleles-other [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (d/q '[:find [?var ...]
                 :in $ ?gene
                 :where
                 [?vh :variation.gene/gene ?gene]
                 [?var :variation/gene ?vh]
                 [?var :variation/allele _]
                 (not [?var :variation/phenotype _])]
               db (:db/id gene))
          (map #(variation-core/process-variation (d/entity db %) (relevant-location gene))))
     :description "alleles currently with no associated phenotype"}))

(defn polymorphisms [gene]
  (let [db (d/entity-db gene)]
    {:data
     (->> (d/q '[:find [?var ...]
                 :in $ ?gene
                 :where
                 [?vh :variation.gene/gene ?gene]
                 [?var :variation/gene ?vh]
                 (not [?var :variation/allele _])
                 (not [?var :variation/phenotype _])]
               db (:db/id gene))
          (map #(variation-core/process-variation (d/entity db %) (relevant-location gene))))
     :description (str "polymorphisms and natural variations "
                       "currently with no associated phenotype")}))
