(ns rest-api.classes.operon
  (:require
    [rest-api.classes.operon.widgets.overview :as overview]
    [rest-api.classes.operon.widgets.feature :as feature]
    [rest-api.classes.operon.widgets.location :as location]
    [rest-api.classes.operon.widgets.structure :as structure]
    [rest-api.classes.operon.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "operon"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :location location/widget
    :structure structure/widget
    :feature feature/widget
    :references references/widget}})
