(ns rest-api.classes.protein
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    ;[rest-api.classes.protein.widgets.overview :as overview]
    [rest-api.classes.protein.widgets.location :as location]
    [rest-api.classes.protein.widgets.history :as history]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "protein"
   :widget
   {;:overview overview/widget
    :location location/widget
    :history history/widget
    :external_links external-links/widget}})
