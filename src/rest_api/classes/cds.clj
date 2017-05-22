(ns rest-api.classes.cds
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "cds"
   :widget
   {:external_links external-links/widget}})
