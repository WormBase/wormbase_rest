(ns rest-api.classes.life-stage
  (:require
    [rest-api.classes.life-stage.widgets.overview :as overview]
    [rest-api.classes.life-stage.widgets.ontology-browser :as ontology-browser]
    [rest-api.classes.life-stage.widgets.expression :as expression]
    [rest-api.classes.life-stage.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "life-stage"
   :widget
   {:overview overview/widget
    :ontology_browser ontology-browser/widget
    :expression expression/widget
    :references references/widget}})
