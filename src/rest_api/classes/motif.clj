(ns rest-api.classes.motif
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.motif.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "motif"
   :widget
   {:overview overview/widget
    :external_links external-links/widget}})
