(ns rest-api.classes.pseudogene
  (:require
    [rest-api.classes.pseudogene.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "pseudogene"
   :widget
   {:overview overview/widget
    }})
