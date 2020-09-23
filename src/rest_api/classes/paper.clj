(ns rest-api.classes.paper
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.paper.widgets.overview :as overview]
    [rest-api.classes.paper.widgets.referenced :as referenced]
    [rest-api.classes.paper.widgets.history :as history]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "paper"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :history history/widget
    :referenced referenced/widget
    :external_links external-links/widget}})
