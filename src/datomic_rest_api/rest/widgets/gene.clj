(ns datomic-rest-api.rest.widgets.gene
  (:require [datomic-rest-api.rest.core :refer [def-rest-widget]]
            [datomic-rest-api.rest.fields.gene :refer :all]))

(def-rest-widget overview [gene]
  {:name                     (name-field gene)
   :version                  (gene-version gene)
   :classification           (gene-classification gene)
   :also_refers_to           (also-refers-to gene)
   :merged_into              (merged-into gene)
   :gene_class               (gene-class gene)
   :concise_description      (concise-description gene)
   :remarks                  (curatorial-remarks gene)
   :operon                   (gene-operon gene)
   :gene_cluster             (gene-cluster gene)
   :other_names              (gene-other-names gene)
   :taxonomy                 (gene-taxonomy gene)
   :status                   (gene-status gene)
   :legacy_information       (legacy-info gene)
   :named_by                 (named-by gene)
   :parent_sequence          (parent-sequence gene)
   :clone                    (parent-clone gene)
   :cloned_by                (cloned-by gene)
   :transposon               (transposon gene)
   :sequence_name            (sequence-name gene)
   :locus_name               (locus-name gene)
   :human_disease_relevance  (disease-relevance gene)
   :structured_description   (structured-description gene)})

(def-rest-widget phenotype [gene]
  {:name                     (name-field gene)
   :drives_overexpression    (drives-overexpression gene)
   :phenotype                (phenotype-field gene)
   :phenotype_not_observed   (phenotype-not-observed-field gene)
   :phenotype_by_interaction (phenotype-by-interaction gene)})

(def-rest-widget genetics [gene]
  {:reference_allele (reference-allele gene)
   :rearrangements   (rearrangements gene)
   :strains          (strains gene)
   :alleles          (alleles gene)
   :alleles_count    (alleles-count gene)
;;   :alleles_other    (alleles-other gene)  ;; can be requested through /rest/field/
;;   :polymorphisms    (polymorphisms gene)  ;; can be requested through /rest/field/
   :name             (name-field gene)})

;; (def-rest-widget mapping-data [gene]
;;   {:name      (name-field gene)

;;    :two_pt_data
;;    {:data (seq (gene-mapping-twopt (d/entity-db gene) (:db/id gene)))
;;     :description "Two point mapping data for this gene"}

;;    :pos_neg_data
;;    {:data (seq (gene-mapping-posneg (d/entity-db gene) (:db/id gene)))
;;     :description "Positive/Negative mapping data for this gene"}

;;    :multi_pt_data
;;    {:data (seq (gene-mapping-multipt (d/entity-db gene) (:db/id gene)))
;;     :description "Multi point mapping data for this gene"}})

;; (def-rest-widget human-diseases [gene]
;;   {:name                    (name-field gene)
;;    :human_disease_relevance (disease-relevance gene)
;;    :human_diseases          (disease-models gene)})

;; (def-rest-widget reagents [gene]
;;   {:name               (name-field gene)
;;    :transgenes         (transgenes gene)
;;    :transgene_products (transgene-products gene)
;;    :microarray_probes  (microarray-probes gene)
;;    :matching_cdnas     (matching-cdnas gene)
;;    :antibodies         (antibodies gene)
;;    :orfeome_primers    (orfeome-primers gene)
;;    :primer_pairs       (primer-pairs gene)
;;    :sage_tags          (sage-tags gene)})

;; (def-rest-widget gene-ontology [gene]
;;   {:name                   (name-field gene)
;;    :gene_ontology_summary  (gene-ontology-summary gene)
;;    :gene_ontology          (gene-ontology-full gene)})

;; (def-rest-widget expression [gene]
;;   {:name                (name-field gene)
;;    :anatomy_terms       (anatomy-terms gene)
;;    :expression_patterns (expression-patterns gene)
;;    :expression_cluster  (expression-clusters gene)
;;    :expression_profiling_graphs (expression-profiling-graphs gene)
;;    :anatomic_expression_patterns (anatomic-expression-patterns gene)
;;    :microarray_topology_map_position (microarray-topology-map-position gene)
;;    :fourd_expression_movies (fourd-expression-movies gene)
;;    :anatomy_function (anatomy-function gene)})

;; (def-rest-widget homology [gene]
;;   {:name                (name-field gene)
;;    :nematode_orthologs  (homology-orthologs gene nematode-species)
;;    :human_orthologs     (homology-orthologs gene ["Homo sapiens"])
;;    :other_orthologs     (homology-orthologs-not gene (conj nematode-species "Homo sapiens"))
;;    :paralogs            (homology-paralogs gene)
;;    :best_blastp_matches (best-blastp-matches gene)
;;    :protein_domains     (protein-domains gene)})

(def-rest-widget history [gene]
  {:name      (name-field gene)
   :history   (history-events gene)
   :old_annot (old-annot gene)})

;; (def-rest-widget sequences [gene]
;;   {:name         (name-field gene)
;;    :gene_models  (gene-models gene)})

;; (def-rest-widget features [gene]
;;   {:feature_image (feature-image gene)
;;    :name       (name-field gene)
;;    :features   (associated-features gene)})

(def-rest-widget external-links [gene]
  {:name  (name-field gene)
   :xrefs (xrefs gene)})
