(ns rest-api.classes.expression-cluster
  (:require
    [rest-api.classes.expression-cluster.widgets.overview :as overview]
    [rest-api.classes.expression-cluster.widgets.associations :as associations]
    [rest-api.classes.expression-cluster.widgets.clustered-data :as clustered-data]
    [rest-api.classes.expression-cluster.widgets.genes :as genes]
    [rest-api.classes.expression-cluster.widgets.regulation :as regulation]
    [rest-api.classes.expression-cluster.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "expression-cluster"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :associations associations/widget
    :clustered_data clustered-data/widget
    :genes genes/widget
    :regulation regulation/widget
    :references references/widget}})
