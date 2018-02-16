(ns rest-api.classes.expr-pattern
  (:require
    [rest-api.classes.expr-pattern.widgets.overview :as overview]
    [rest-api.classes.expr-pattern.widgets.details :as details]
    [rest-api.classes.expr-pattern.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "expr-pattern"
   :widget
   {:overview overview/widget
    :details details/widget
    :references references/widget}})
