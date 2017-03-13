(ns rest-api.classes.homology-group
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "homology-group"
   :widget
   {:external_links external-links/widget}})
