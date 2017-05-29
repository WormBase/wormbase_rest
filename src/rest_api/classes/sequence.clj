(ns rest-api.classes.sequence
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.sequence.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "sequence"
   :widget
   {:overview overview/widget
    :external_links external-links/widget}})
