(ns rest-api.classes.clone
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.clone.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "clone"
   :widget
   {:overview overview/widget
    :external_links external-links/widget}})
