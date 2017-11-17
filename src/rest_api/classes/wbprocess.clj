(ns rest-api.classes.wbprocess
  (:require
    [rest-api.classes.wbprocess.widgets.overview :as overview]
    [rest-api.classes.wbprocess.widgets.pathways :as pathways]
    [rest-api.classes.wbprocess.widgets.molecule :as molecule]
    [rest-api.classes.wbprocess.widgets.genes :as genes]
    [rest-api.classes.wbprocess.widgets.go-term :as go-term]
    [rest-api.classes.wbprocess.widgets.phenotypes :as phenotypes]
    [rest-api.classes.wbprocess.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "wbprocess"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget
    :pathways pathways/widget
    :molecule molecule/widget
    :go_term go-term/widget
    :genes genes/widget
    :references references/widget}})
