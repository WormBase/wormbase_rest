(ns rest-api.classes.protein
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "protein"
   :widget
   {:external_links external-links/widget}})
