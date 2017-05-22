(ns rest-api.classes.sequence
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "sequence"
   :widget
   {:external_links external-links/widget}})
