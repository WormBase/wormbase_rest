(ns rest-api.classes.paper
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.paper.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "paper"
   :widget
   {:overview overview/widget
    :external_links external-links/widget}})
