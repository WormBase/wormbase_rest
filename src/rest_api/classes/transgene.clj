(ns rest-api.classes.transgene
  (:require
    [rest-api.classes.transgene.widgets.phenotypes :as phenotypes]
    [rest-api.classes.transgene.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transgene"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget}})
