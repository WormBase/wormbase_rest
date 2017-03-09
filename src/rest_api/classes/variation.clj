(ns rest-api.classes.variation
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "variation"
   :widget
   {:external_links external-links/widget}})
