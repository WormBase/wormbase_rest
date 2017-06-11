(ns rest-api.classes.expr-profile
  (:require
    ;[rest-api.classes.expr-profile.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "expr-profile"
   :widget
   {;:overview overview/widget
    }})
