(ns rest-api.classes.operon
  (:require
    [rest-api.classes.operon.widgets.overview :as overview]
    [rest-api.classes.operon.widgets.location :as location]
    [rest-api.classes.operon.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "operon"
   :widget
   {:overview overview/widget
    :location location/widget
    :references references/widget}})
