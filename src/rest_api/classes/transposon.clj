(ns rest-api.classes.transposon
  (:require
    [rest-api.classes.transposon.widgets.overview :as overview]
    [rest-api.classes.transposon.widgets.location :as location]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transposon"
   :widget
   {:overview overview/widget
    :location location/widget}})
