(ns rest-api.classes.variation
  (:require
    [rest-api.classes.variation.widgets.overview :as overview]
    [rest-api.classes.variation.widgets.phenotypes :as phenotypes]
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "variation"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget
    :external_links external-links/widget}})
