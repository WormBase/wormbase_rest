(ns rest-api.classes.analysis
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "analysis"
   :widget
   {:external_links external-links/widget}})
