(ns rest-api.classes.transgene
  (:require
    [rest-api.classes.transgene.widgets.phenotypes :as phenotypes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transgene"
   :widget
   {:phenotypes phenotypes/widget}})
