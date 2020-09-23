(ns rest-api.classes.sequence
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.sequence.widgets.overview :as overview]
    [rest-api.classes.sequence.widgets.location :as location]
    [rest-api.classes.sequence.widgets.reagents :as reagents]
    [rest-api.classes.sequence.widgets.sequences :as sequences]
    [rest-api.classes.sequence.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "sequence"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :external_links external-links/widget
    :location location/widget
    :reagents reagents/widget
    :sequences sequences/widget
    :references references/widget}})
