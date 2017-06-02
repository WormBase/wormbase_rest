(ns rest-api.classes.gene-cluster
  (:require
    ;[rest-api.classes.gene-cluster.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "gene-cluster"
   :widget
   {;:overview overview/widget}})
