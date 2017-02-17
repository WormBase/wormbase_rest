(ns rest-api.classes.variation
  (:require
    [rest-api.classes.variation.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "variation"
   :widget
   {:overview overview/widget}})
