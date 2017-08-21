(ns rest-api.classes.gene-class
  (:require
    [rest-api.classes.gene-class.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "gene-class"
   :widget
   {:overview overview/widget}})
