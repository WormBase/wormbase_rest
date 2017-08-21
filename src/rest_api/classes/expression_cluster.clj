(ns rest-api.classes.expression-cluster
  (:require
    [rest-api.classes.expression-cluster.widgets.overview :as overview]
    [rest-api.classes.expression-cluster.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "expression-cluster"
   :widget
   {:overview overview/widget
    :references references/widget}})
