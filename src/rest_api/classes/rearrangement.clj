(ns rest-api.classes.rearrangement
  (:require
    [rest-api.classes.rearrangement.widgets.overview :as overview]
    [rest-api.classes.rearrangement.widgets.phenotypes :as phenotypes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "rearrangement"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget}})
