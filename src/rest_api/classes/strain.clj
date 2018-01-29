(ns rest-api.classes.strain
  (:require
    [rest-api.classes.strain.widgets.phenotypes :as phenotypes]
    [rest-api.classes.strain.widgets.overview :as overview]
    [rest-api.classes.strain.widgets.contains :as contains]
    [rest-api.classes.strain.widgets.origin :as origin]
    [rest-api.classes.strain.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "strain"
   :widget
   {:overview overview/widget
    :contains contains/widget
    :origin origin/widget
    :phenotypes phenotypes/widget
    :references references/widget}})
