(ns rest-api.classes.variation
  (:require
    [rest-api.classes.variation.widgets.overview :as overview]
    [rest-api.classes.variation.widgets.genetics :as genetics]
    [rest-api.classes.variation.widgets.isolation :as isolation]
    [rest-api.classes.variation.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "variation"
   :widget
   {:overview overview/widget
    :genetics genetics/widget
    :isolation isolation/widget
    :external-links external-links/widget}})
