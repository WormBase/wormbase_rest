(ns datomic-rest-api.rest.gene.routing
  (:require
   [datomic-rest-api.routes :as routes]
   [datomic-rest-api.rest.gene.widgets.external-links :as external-links]
   [datomic-rest-api.rest.gene.widgets.feature :as feature]
   [datomic-rest-api.rest.gene.widgets.genetics :as genetics]
   [datomic-rest-api.rest.gene.widgets.history :as history]
   [datomic-rest-api.rest.gene.widgets.mapping-data :as mapping-data]
   [datomic-rest-api.rest.gene.widgets.ontology :as gene-ontology]
   [datomic-rest-api.rest.gene.widgets.overview :as overview]
   [datomic-rest-api.rest.gene.widgets.phenotypes :as phenotypes]
   [datomic-rest-api.rest.gene.variation :as variation]))

(routes/defroutes-spec
  {:datatype "gene"
   :widget
   {:external_links external-links/widget
    :feature feature/widget
    :gene_ontology gene-ontology/widget
    :genetics genetics/widget
    :history history/widget
    :mapping_data mapping-data/widget
    :overview overview/widget
    :phenotypes phenotypes/widget}
   :field
   {:alles_other variation/alleles-other
    :polymorphisms variation/polymorphisms}})




  
