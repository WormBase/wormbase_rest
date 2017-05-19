(ns rest-api.classes.expr-pattern
  (:require
    [rest-api.classes.expr-pattern.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "expr-pattern"
   :widget
   {:overview overview/widget}})
