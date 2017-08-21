(ns rest-api.classes.rnai
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.rnai.widgets.phenotypes :as phenotypes]
    [rest-api.classes.rnai.widgets.overview :as overview]
    [rest-api.classes.rnai.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "rnai"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget
    :references references/widget
    :external_links external-links/widget}})
