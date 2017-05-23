(ns rest-api.classes.rnai
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.rnai.widgets.phenotypes :as phenotypes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "rnai"
   :widget
   {:phenotypes phenotypes/widget
    :external_links external-links/widget}})
