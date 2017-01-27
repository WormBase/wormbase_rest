(ns datomic-rest-api.rest.widgets.gene
  (:require [datomic-rest-api.rest.core :refer [def-rest-routes widget-setting field-setting]]
            [datomic-rest-api.rest.fields.gene :as gene-fields]
            [compojure.api.sweet :as sweet]))

(def-rest-routes routes "gene"
  (widget-setting "overview"
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

  (widget-setting "phenotype"
                  {:name                     gene-fields/name-field
                   :drives_overexpression    gene-fields/drives-overexpression
                   :phenotype                gene-fields/phenotype-field
                   :phenotype_not_observed   gene-fields/phenotype-not-observed-field
                   :phenotype_by_interaction gene-fields/phenotype-by-interaction})
  (widget-setting "genetics"
                  {:reference_allele gene-fields/reference-allele
                   :rearrangements   gene-fields/rearrangements
                   :strains          gene-fields/strains
                   :alleles          gene-fields/alleles
                   :alleles_count    gene-fields/alleles-count
                   :name             gene-fields/name-field})
  (widget-setting "mapping-data"
                  {:name      gene-fields/name-field
                   :two_pt_data gene-fields/gene-mapping-twopt
                   :pos_neg_data gene-fields/gene-mapping-posneg
                   :multi_pt_data gene-fields/gene-mapping-multipt})
  (widget-setting "gene-ontology"
                  {:name                   gene-fields/name-field
                   :gene_ontology_summary  gene-fields/gene-ontology-summary
                   :gene_ontology          gene-fields/gene-ontology-full})
  (widget-setting "history"
                  {:name      gene-fields/name-field
                   :history   gene-fields/history-events
                   :old_annot gene-fields/old-annot})
  (widget-setting "feature"
                  {:feature_image gene-fields/feature-image
                   :name          gene-fields/name-field
                   :features      gene-fields/associated-features})
  (widget-setting "external-links"
                  {:name  gene-fields/name-field
                   :xrefs gene-fields/xrefs})
  (field-setting "fpkm_expression_summary_ls" gene-fields/fpkm-expression-summary-ls)
  (field-setting "alleles_other" gene-fields/alleles-other)
  (field-setting "polymorphisms" gene-fields/polymorphisms))
