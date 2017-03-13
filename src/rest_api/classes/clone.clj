(ns rest-api.classes.clone
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "clone"
   :widget
   {:external_links external-links/widget}})
