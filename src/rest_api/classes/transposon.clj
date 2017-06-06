(ns rest-api.classes.transposon
  (:require
    [rest-api.classes.transposon.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transposon"
   :widget
   {:overview overview/widget}})
