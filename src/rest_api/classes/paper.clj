(ns rest-api.classes.paper
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "paper"
   :widget
   {:external_links external-links/widget}})
