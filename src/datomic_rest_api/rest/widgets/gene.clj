(ns datomic-rest-api.rest.widgets.gene
  (:require [datomic-rest-api.rest.core :refer [def-rest-widget]]
            [datomic-rest-api.rest.fields.gene :as gene-fields]))

(def-rest-widget overview [gene]
  {:name                     (gene-fields/name-field gene)
   :version                  (gene-fields/gene-version gene)
   :classification           (gene-fields/gene-classification gene)
   :also_refers_to           (gene-fields/also-refers-to gene)
   :merged_into              (gene-fields/merged-into gene)
   :gene_class               (gene-fields/gene-class gene)
   :concise_description      (gene-fields/concise-description gene)
   :remarks                  (gene-fields/curatorial-remarks gene)
   :operon                   (gene-fields/gene-operon gene)
   :gene_cluster             (gene-fields/gene-cluster gene)
   :other_names              (gene-fields/gene-other-names gene)
   :taxonomy                 (gene-fields/gene-taxonomy gene)
   :status                   (gene-fields/gene-status gene)
   :legacy_information       (gene-fields/legacy-info gene)
   :named_by                 (gene-fields/named-by gene)
   :parent_sequence          (gene-fields/parent-sequence gene)
   :clone                    (gene-fields/parent-clone gene)
   :cloned_by                (gene-fields/cloned-by gene)
   :transposon               (gene-fields/transposon gene)
   :sequence_name            (gene-fields/sequence-name gene)
   :locus_name               (gene-fields/locus-name gene)
   :human_disease_relevance  (gene-fields/disease-relevance gene)
   :structured_description   (gene-fields/structured-description gene)})

(def-rest-widget phenotype [gene]
  {:name                     (gene-fields/name-field gene)
   :drives_overexpression    (gene-fields/drives-overexpression gene)
   :phenotype                (gene-fields/phenotype-field gene)
   :phenotype_not_observed   (gene-fields/phenotype-not-observed-field gene)
   :phenotype_by_interaction (gene-fields/phenotype-by-interaction gene)})

(def-rest-widget genetics [gene]
  {:reference_allele (gene-fields/reference-allele gene)
   :rearrangements   (gene-fields/rearrangements gene)
   :strains          (gene-fields/strains gene)
   :alleles          (gene-fields/alleles gene)
   :alleles_count    (gene-fields/alleles-count gene)
;;   :alleles_other    (gene-fields/alleles-other gene)  ;; can be requested through /rest/field/
;;   :polymorphisms    (gene-fields/polymorphisms gene)  ;; can be requested through /rest/field/
   :name             (gene-fields/name-field gene)})

(def-rest-widget mapping-data [gene]
  {:name      (gene-fields/name-field gene)

   :two_pt_data
   {:data (seq (gene-fields/gene-mapping-twopt gene))
    :description "Two point mapping data for this gene"}

   :pos_neg_data
   {:data (seq (gene-fields/gene-mapping-posneg gene))
    :description "Positive/Negative mapping data for this gene"}

   :multi_pt_data
   {:data (seq (gene-fields/gene-mapping-multipt gene))
    :description "Multi point mapping data for this gene"}})

;; (def-rest-widget human-diseases [gene]
;;   {:name                    (gene-fields/name-field gene)
;;    :human_disease_relevance (gene-fields/disease-relevance gene)
;;    :human_diseases          (gene-fields/disease-models gene)})

;; (def-rest-widget reagents [gene]
;;   {:name               (gene-fields/name-field gene)
;;    :transgenes         (gene-fields/transgenes gene)
;;    :transgene_products (gene-fields/transgene-products gene)
;;    :microarray_probes  (gene-fields/microarray-probes gene)
;;    :matching_cdnas     (gene-fields/matching-cdnas gene)
;;    :antibodies         (gene-fields/antibodies gene)
;;    :orfeome_primers    (gene-fields/orfeome-primers gene)
;;    :primer_pairs       (gene-fields/primer-pairs gene)
;;    :sage_tags          (gene-fields/sage-tags gene)})

;; (def-rest-widget gene-ontology [gene]
;;   {:name                   (gene-fields/name-field gene)
;;    :gene_ontology_summary  (gene-fields/gene-ontology-summary gene)
;;    :gene_ontology          (gene-fields/gene-ontology-full gene)})

;; (def-rest-widget expression [gene]
;;   {:name                (gene-fields/name-field gene)
;;    :anatomy_terms       (gene-fields/anatomy-terms gene)
;;    :expression_patterns (gene-fields/expression-patterns gene)
;;    :expression_cluster  (gene-fields/expression-clusters gene)
;;    :expression_profiling_graphs (gene-fields/expression-profiling-graphs gene)
;;    :anatomic_expression_patterns (gene-fields/anatomic-expression-patterns gene)
;;    :microarray_topology_map_position (gene-fields/microarray-topology-map-position gene)
;;    :fourd_expression_movies (gene-fields/fourd-expression-movies gene)
;;    :anatomy_function (gene-fields/anatomy-function gene)})

;; (def-rest-widget homology [gene]
;;   {:name                (gene-fields/name-field gene)
;;    :nematode_orthologs  (gene-fields/homology-orthologs gene nematode-species)
;;    :human_orthologs     (gene-fields/homology-orthologs gene ["Homo sapiens"])
;;    :other_orthologs     (gene-fields/homology-orthologs-not gene (conj nematode-species "Homo sapiens"))
;;    :paralogs            (gene-fields/homology-paralogs gene)
;;    :best_blastp_matches (gene-fields/best-blastp-matches gene)
;;    :protein_domains     (gene-fields/protein-domains gene)})

(def-rest-widget history [gene]
  {:name      (gene-fields/name-field gene)
   :history   (gene-fields/history-events gene)
   :old_annot (gene-fields/old-annot gene)})

;; (def-rest-widget sequences [gene]
;;   {:name         (gene-fields/name-field gene)
;;    :gene_models  (gene-fields/gene-models gene)})

;; (def-rest-widget features [gene]
;;   {:feature_image (gene-fields/feature-image gene)
;;    :name       (gene-fields/name-field gene)
;;    :features   (gene-fields/associated-features gene)})

(def-rest-widget external-links [gene]
  {:name  (gene-fields/name-field gene)
   :xrefs (gene-fields/xrefs gene)})
