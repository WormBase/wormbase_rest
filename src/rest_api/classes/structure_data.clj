(ns rest-api.classes.structure-data
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "structure-data"
   :widget
   {:external_links external-links/widget}})
