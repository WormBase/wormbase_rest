(ns rest-api.classes.rearrangement
  (:require
    [rest-api.classes.rearrangement.widgets.overview :as overview]
    [rest-api.classes.rearrangement.widgets.phenotypes :as phenotypes]
    [rest-api.classes.rearrangement.widgets.isolation :as isolation]
    [rest-api.classes.rearrangement.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "rearrangement"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :phenotypes phenotypes/widget
    :isolation isolation/widget
    :references references/widget}})
