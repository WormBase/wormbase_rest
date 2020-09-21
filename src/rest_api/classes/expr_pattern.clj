(ns rest-api.classes.expr-pattern
  (:require
    [rest-api.classes.expr-pattern.widgets.overview :as overview]
    [rest-api.classes.expr-pattern.widgets.details :as details]
    [rest-api.classes.expr-pattern.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "expr-pattern"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :details details/widget
    :references references/widget}})
