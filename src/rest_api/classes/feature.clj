(ns rest-api.classes.feature
  (:require
    [rest-api.classes.feature.widgets.overview :as overview]
    [rest-api.classes.feature.widgets.history :as history]
    [rest-api.classes.feature.widgets.evidence :as evidence]
    [rest-api.classes.feature.widgets.associations :as associations]
    [rest-api.classes.feature.widgets.location :as location]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "feature"
   :widget
   {:overview overview/widget
    :history history/widget
    :associations associations/widget
    :evidence evidence/widget
    :location location/widget}})
