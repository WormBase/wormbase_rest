(ns rest-api.classes.wbprocess
  (:require
    [rest-api.classes.wbprocess.widgets.overview :as overview]
    [rest-api.classes.wbprocess.widgets.phenotypes :as phenotypes]
    [rest-api.classes.wbprocess.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "wbprocess"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget
    :references references/widget}})
