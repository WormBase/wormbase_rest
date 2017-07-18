(ns rest-api.classes.laboratory
  (:require
    [rest-api.classes.laboratory.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "laboratory"
   :widget
   {:overview overview/widget}})
