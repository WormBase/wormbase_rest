(ns rest-api.classes.pseudogene
  (:require
    [rest-api.classes.pseudogene.widgets.overview :as overview]
    [rest-api.classes.pseudogene.widgets.feature :as feature]
    [rest-api.classes.pseudogene.widgets.genetics :as genetics]
    [rest-api.classes.pseudogene.widgets.reagents :as reagents]
    [rest-api.classes.pseudogene.widgets.expression :as expression]
    [rest-api.classes.pseudogene.widgets.location :as location]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "pseudogene"
   :widget
   {:overview overview/widget
    :feature feature/widget
    :genetics genetics/widget
    :reagents reagents/widget
    :expression expression/widget
    :location location/widget}})
