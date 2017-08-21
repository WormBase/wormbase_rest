(ns rest-api.classes.analysis
  (:require
    [rest-api.classes.analysis.widgets.overview :as overview]
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.analysis.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "analysis"
   :widget
   {:overview overview/widget
    :external_links external-links/widget
    :references references/widget}})
