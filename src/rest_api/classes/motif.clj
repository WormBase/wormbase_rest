(ns rest-api.classes.motif
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "motif"
   :widget
   {:external_links external-links/widget}})
