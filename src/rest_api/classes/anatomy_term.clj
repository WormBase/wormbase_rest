(ns rest-api.classes.anatomy-term
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "anatomy-term"
   :widget
   {:external_links external-links/widget}})
