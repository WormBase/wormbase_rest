(ns rest-api.classes.molecule
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "molecule"
   :widget
   {:external_links external-links/widget}})
