(ns rest-api.classes.strain
  (:require
    [rest-api.classes.strain.widgets.phenotypes :as phenotypes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "strain"
   :widget
   {:phenotypes phenotypes/widget}})
