(ns rest-api.classes.transposon-family
  (:require
    [rest-api.classes.transposon-family.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transposon-family"
   :widget
   {:overview overview/widget}})
