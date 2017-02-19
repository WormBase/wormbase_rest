(ns rest-api.classes.variation.widgets.genetics
  (:require
    [datomic.api :as d]
    [rest-api.classes.generic :as global-generic]
    [rest-api.classes.variation.generic :as generic]
    [rest-api.classes.gene.widgets.genetics :as gene-genetics]
    [rest-api.classes.gene.variation :as gene-variation]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn- get-gene-from-variation [variation]
  (seq (map :variation.gene/gene (:variation/gene variation))))

(defn gene-class [variation]
  {:data (if-let [gene-class (:gene-class variation)]
             (map (pack-obj) gene-class))
   :description "the class of the gene the variation falls in, if any"})

(defn corresponding-gene [variation]
  {:data (if-let [gene (:corresponding-transgene variation)]
             (map (pack-obj) gene))
   :description "gene in which this variation is found (if any)"})

(defn reference-allele [variation]
  (let [genes (get-gene-from-variation variation)
        gene (first genes)]
  (gene-genetics/reference-allele gene)))

(defn other-allele [variation]
  {:data (if-let [gene (first (:variation/gene variation))]
           (let [db  (d/entity-db gene)]
	   (->> (d/q '[:find [?var ...]
		       :in $ ?variation ?gene
	 	       :where
	    	       [?var :variation/strain ?vsh]
                       [?vsh :variation.strain/strain strain]
		       [?var :variation/phenotype _]
                       (not [?var :variation/id variation])]
		     db (:db/id variation) (:db/id gene))
		(map #(gene-variation/process-variation (d/entity db %))))))
  :description "other alleles of the containing gene (if known)"})

(defn linked-to [variation]
  {:data (if-let [linked-to (:variation/linked-to variation)]
           "found")
  :description "linked_to"})

(defn strain [variation]
  {:data (if-let [strains (:variation/strain variation)]
           (global-generic/categorize-strains strains))
   :description "strains carrying this variation"})

(defn rescued-by-transgene [variation]
  {:data nil
   :description "transgenes that rescue phenotype(s) caused by this variation"})

(def widget
  {:name                 generic/name-field
   :gene_class           gene-class
   :corresponding_gene   corresponding-gene
   :reference_allele     reference-allele
   :other_allele         other-allele
   :linked_to            linked-to
   :strain               strain
   :rescued_by_transgene rescued-by-transgene})
