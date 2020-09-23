(ns rest-api.classes.position-matrix
  (:require
    [rest-api.classes.position-matrix.widgets.overview :as overview]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "position-matrix"
   :widget
   {:overview overview/widget
    :graphview graphview/widget}})
