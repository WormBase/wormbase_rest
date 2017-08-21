(ns rest-api.classes.go-term
  (:require
    [rest-api.classes.go-term.widgets.overview :as overview]
    [rest-api.classes.go-term.widgets.ontology-browser :as ontology-browser]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "go-term"
   :widget
   {:overview overview/widget
    :ontology_browser ontology-browser/widget}})
