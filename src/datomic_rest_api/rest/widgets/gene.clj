(ns datomic-rest-api.rest.widgets.gene
  (:require [datomic-rest-api.rest.core :refer [def-rest-widget register-independent-field]]
            [datomic-rest-api.rest.fields.gene :as gene-fields]))

(def-rest-widget overview
  {:name                     gene-fields/name-field
   :version                  gene-fields/gene-version
   :classification           gene-fields/gene-classification
   :also_refers_to           gene-fields/also-refers-to
   :merged_into              gene-fields/merged-into
   :gene_class               gene-fields/gene-class
   :concise_description      gene-fields/concise-description
   :remarks                  gene-fields/curatorial-remarks
   :operon                   gene-fields/gene-operon
   :gene_cluster             gene-fields/gene-cluster
   :other_names              gene-fields/gene-other-names
   :taxonomy                 gene-fields/gene-taxonomy
   :status                   gene-fields/gene-status
   :legacy_information       gene-fields/legacy-info
   :named_by                 gene-fields/named-by
   :parent_sequence          gene-fields/parent-sequence
   :clone                    gene-fields/parent-clone
   :cloned_by                gene-fields/cloned-by
   :transposon               gene-fields/transposon
   :sequence_name            gene-fields/sequence-name
   :locus_name               gene-fields/locus-name
   :human_disease_relevance  gene-fields/disease-relevance
   :structured_description   gene-fields/structured-description})

(def-rest-widget phenotype
  {:name                     gene-fields/name-field
   :drives_overexpression    gene-fields/drives-overexpression
   :phenotype                gene-fields/phenotype-field
   :phenotype_not_observed   gene-fields/phenotype-not-observed-field
   :phenotype_by_interaction gene-fields/phenotype-by-interaction})

(def-rest-widget genetics
  {:reference_allele gene-fields/reference-allele
   :rearrangements   gene-fields/rearrangements
   :strains          gene-fields/strains
   :alleles          gene-fields/alleles
   :alleles_count    gene-fields/alleles-count
;;   :alleles_other    gene-fields/alleles-other  ;; can be requested through /rest/field/
;;   :polymorphisms    gene-fields/polymorphisms  ;; can be requested through /rest/field/
   :name             gene-fields/name-field})

(def-rest-widget mapping-data
  {:name      gene-fields/name-field
   :two_pt_data gene-fields/gene-mapping-twopt
   :pos_neg_data gene-fields/gene-mapping-posneg
   :multi_pt_data gene-fields/gene-mapping-multipt})

;; (def-rest-widget human-diseases
;;   {:name                    gene-fields/name-field
;;    :human_disease_relevance gene-fields/disease-relevance
;;    :human_diseases          gene-fields/disease-models})

;; (def-rest-widget reagents
;;   {:name               gene-fields/name-field
;;    :transgenes         gene-fields/transgenes
;;    :transgene_products gene-fields/transgene-products
;;    :microarray_probes  gene-fields/microarray-probes
;;    :matching_cdnas     gene-fields/matching-cdnas
;;    :antibodies         gene-fields/antibodies
;;    :orfeome_primers    gene-fields/orfeome-primers
;;    :primer_pairs       gene-fields/primer-pairs
;;    :sage_tags          gene-fields/sage-tags})

(def-rest-widget gene-ontology
  {:name                   gene-fields/name-field
   :gene_ontology_summary  gene-fields/gene-ontology-summary
   :gene_ontology          gene-fields/gene-ontology-full})

;; (def-rest-widget expression
;;   {:name                gene-fields/name-field
;;    :anatomy_terms       gene-fields/anatomy-terms
;;    :expression_patterns gene-fields/expression-patterns
;;    :expression_cluster  gene-fields/expression-clusters
;;    :expression_profiling_graphs gene-fields/expression-profiling-graphs
;;    :anatomic_expression_patterns gene-fields/anatomic-expression-patterns
;;    :microarray_topology_map_position gene-fields/microarray-topology-map-position
;;    :fourd_expression_movies gene-fields/fourd-expression-movies
;;    :anatomy_function gene-fields/anatomy-function})

;; (def-rest-widget homology
;;   {:name                gene-fields/name-field
;;    :nematode_orthologs  (gene-fields/homology-orthologs gene nematode-species)
;;    :human_orthologs     (gene-fields/homology-orthologs gene ["Homo sapiens"])
;;    :other_orthologs     (gene-fields/homology-orthologs-not gene (conj nematode-species "Homo sapiens"))
;;    :paralogs            gene-fields/homology-paralogs
;;    :best_blastp_matches gene-fields/best-blastp-matches
;;    :protein_domains     gene-fields/protein-domains})

(def-rest-widget history
  {:name      gene-fields/name-field
   :history   gene-fields/history-events
   :old_annot gene-fields/old-annot})

;; (def-rest-widget sequences
;;   {:name         gene-fields/name-field
;;    :gene_models  gene-fields/gene-models})

(def-rest-widget feature
  {:feature_image gene-fields/feature-image
   :name          gene-fields/name-field
   :features      gene-fields/associated-features})

(def-rest-widget external-links
  {:name  gene-fields/name-field
   :xrefs gene-fields/xrefs})


(register-independent-field "fpkm_expression_summary_ls" gene-fields/fpkm-expression-summary-ls)
(register-independent-field "alleles_other" gene-fields/alleles-other)
(register-independent-field "polymorphisms" gene-fields/polymorphisms)
