(ns rest-api.classes.rearrangement
  (:require
    [rest-api.classes.rearrangement.widgets.phenotypes :as phenotypes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "rearrangement"
   :widget
   {:phenotypes phenotypes/widget}})
