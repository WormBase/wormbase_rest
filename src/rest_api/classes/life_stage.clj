(ns rest-api.classes.life-stage
  (:require
    [rest-api.classes.life-stage.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "life-stage"
   :widget
   {:overview overview/widget}})
