(ns rest-api.classes.gene
  (:require
   [rest-api.classes.gene.widgets.external-links :as external-links]
   [rest-api.classes.gene.widgets.expression :as expression]
   [rest-api.classes.gene.widgets.feature :as feature]
   [rest-api.classes.gene.widgets.sequences :as sequences]
   [rest-api.classes.gene.widgets.genetics :as genetics]
   [rest-api.classes.gene.widgets.history :as history]
   [rest-api.classes.interaction.core :as interactions]
   [rest-api.classes.gene.widgets.mapping-data :as mapping-data]
   [rest-api.classes.gene.widgets.ontology :as gene-ontology]
   [rest-api.classes.gene.widgets.references :as references]
   [rest-api.classes.gene.widgets.overview :as overview]
   [rest-api.classes.gene.widgets.phenotype :as phenotype]
   [rest-api.classes.gene.widgets.location :as location]
   [rest-api.classes.gene.widgets.human-diseases :as human-diseases]
   [rest-api.classes.gene.widgets.homology :as homology]
   [rest-api.classes.gene.widgets.reagents :as reagents]
   [rest-api.classes.gene.widgets.phenotype-graph :as phenotype-graph]
   [rest-api.classes.gene.variation :as variation]
   [rest-api.classes.graphview.widget :as graphview]
   [rest-api.classes.gene.expression :as exp]
   [rest-api.classes.gene.widgets.biocyc :as biocyc]
   [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "gene"
   :widget
   {:external_links external-links/widget
    :graphview graphview/widget
    :expression expression/widget
    :feature feature/widget
    :gene_ontology gene-ontology/widget
    :genetics genetics/widget
    :history history/widget
    :interactions interactions/widget
    :mapping_data mapping-data/widget
    :overview overview/widget
    :location location/widget
    :homology homology/widget
    :human_diseases human-diseases/widget
    :sequences sequences/widget
    :references references/widget
    :reagents reagents/widget
    :phenotype_graph phenotype-graph/widget
    :phenotype phenotype/widget
    :biocyc biocyc/widget}
   :field
   {:alleles_other variation/alleles-other
    :interaction_details interactions/interaction-details
    :polymorphisms variation/polymorphisms
    :fpkm_expression_summary_ls exp/fpkm-expression-summary-ls
    :phenotype_flat phenotype/phenotype-field-flat
    :phenotype_not_observed_flat phenotype/phenotype-not-observed-field-flat
    :drives_overexpression_flat phenotype/drives-overexpression-flat}})
