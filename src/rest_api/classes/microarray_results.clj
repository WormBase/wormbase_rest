(ns rest-api.classes.microarray-results
  (:require
    [rest-api.classes.microarray-results.widgets.overview :as overview]
    [rest-api.classes.microarray-results.widgets.experiments :as experiments]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "microarray-results"
   :widget
   {:overview overview/widget
    :experiments experiments/widget}})
