(ns rest-api.classes.homology-group
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.homology-group.widgets.overview :as overview]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "homology-group"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :external_links external-links/widget}})
