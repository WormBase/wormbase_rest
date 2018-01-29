(ns rest-api.classes.cds
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    ;[rest-api.classes.cds.widgets.overview :as overview]
    [rest-api.classes.cds.widgets.location :as location]
    [rest-api.classes.cds.widgets.feature :as feature]
    ;[rest-api.classes.cds.widgets.reagents :as reagents]
    [rest-api.classes.cds.widgets.references :as references]
    [rest-api.classes.gene.expression :as gene-expression]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "cds"
   :widget
   {;:overview overview/widget
    :location location/widget
    :feature feature/widget
    ;:reagents reagents/widget
    :external_links external-links/widget
    :references references/widget}
   :field
   {:fpkm_expression_summary_ls gene-expression/fpkm-expression-summary-ls}})
