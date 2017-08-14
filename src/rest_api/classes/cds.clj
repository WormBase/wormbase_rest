(ns rest-api.classes.cds
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.cds.widgets.overview :as overview]
    [rest-api.classes.cds.widgets.location :as location]
    [rest-api.classes.cds.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "cds"
   :widget
   {:overview overview/widget
    :location location/widget
    :external_links external-links/widget
    :references references/widget}})
