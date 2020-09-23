(ns rest-api.classes.structure-data
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    ;[rest-api.classes.structure-data.widgets.overview :as overview]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "structure-data"
   :widget
   {;:overview overview/widget
    :graphview graphview/widget
    :external_links external-links/widget}})
