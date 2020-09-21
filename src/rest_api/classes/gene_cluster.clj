(ns rest-api.classes.gene-cluster
  (:require
    [rest-api.classes.gene-cluster.widgets.overview :as overview]
    [rest-api.classes.gene-cluster.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "gene-cluster"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :references references/widget}})
