(ns rest-api.classes.rnai
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "rnai"
   :widget
   {:external_links external-links/widget}})