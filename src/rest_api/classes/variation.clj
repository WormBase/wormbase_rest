(ns rest-api.classes.variation
  (:require
    [rest-api.classes.person.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "person"
   :widget
   {
    :overview overview/widget}})
