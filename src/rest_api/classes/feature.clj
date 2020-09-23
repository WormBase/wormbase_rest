(ns rest-api.classes.feature
  (:require
    [rest-api.classes.feature.widgets.overview :as overview]
    [rest-api.classes.feature.widgets.history :as history]
    [rest-api.classes.feature.widgets.evidence :as evidence]
    [rest-api.classes.feature.widgets.molecular-details :as molecular-details]
    [rest-api.classes.feature.widgets.associations :as associations]
    [rest-api.classes.feature.widgets.location :as location]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "feature"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :history history/widget
    :associations associations/widget
    :molecular_details molecular-details/widget
    :evidence evidence/widget
    :location location/widget}})
