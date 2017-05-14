(ns rest-api.classes.antibody
  (:require
    [rest-api.classes.antibody.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "antibody"
   :widget
   {:overview overview/widget}})
