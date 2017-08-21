(ns rest-api.classes.interaction
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.interaction.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "interaction"
   :widget
   {:overview overview/widget
    :external_links external-links/widget}})
