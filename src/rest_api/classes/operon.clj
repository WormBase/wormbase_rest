(ns rest-api.classes.operon
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.operon.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "operon"
   :widget
   {:overview overview/widget}})
