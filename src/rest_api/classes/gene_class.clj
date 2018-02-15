(ns rest-api.classes.gene-class
  (:require
    [rest-api.classes.gene-class.widgets.overview :as overview]
    [rest-api.classes.gene-class.widgets.current-genes :as current-genes]
    [rest-api.classes.gene-class.widgets.previous-genes :as previous-genes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "gene-class"
   :widget
   {:overview overview/widget
    :current_genes current-genes/widget
    :previous_genes previous-genes/widget}})
