(ns rest-api.classes.protein
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.protein.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "protein"
   :widget
   {:overview overview/widget
    :external_links external-links/widget}})
