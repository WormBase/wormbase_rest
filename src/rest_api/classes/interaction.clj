(ns rest-api.classes.interaction
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "interaction"
   :widget
   {:external_links external-links/widget}})
