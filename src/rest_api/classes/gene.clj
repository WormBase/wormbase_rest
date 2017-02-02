(ns rest-api.classes.gene
  (:require
   [rest-api.classes.gene.widgets.external-links :as external-links]
   [rest-api.classes.gene.widgets.feature :as feature]
   [rest-api.classes.gene.widgets.sequences :as sequences]
   [rest-api.classes.gene.widgets.genetics :as genetics]
   [rest-api.classes.gene.widgets.history :as history]
   [rest-api.classes.gene.widgets.mapping-data :as mapping-data]
   [rest-api.classes.gene.widgets.ontology :as gene-ontology]
   [rest-api.classes.gene.widgets.overview :as overview]
   [rest-api.classes.gene.widgets.phenotypes :as phenotypes]
   [rest-api.classes.gene.variation :as variation]
   [rest-api.routing :as routing]))

(routing/defroutes
  {:datatype "gene"
   :widget
   {:external_links external-links/widget
    :feature feature/widget
    :gene_ontology gene-ontology/widget
    :genetics genetics/widget
    :history history/widget
    :mapping_data mapping-data/widget
    :overview overview/widget
    :sequences sequences/widget
    :phenotypes phenotypes/widget}
   :field
   {:alles_other variation/alleles-other
    :polymorphisms variation/polymorphisms}})
