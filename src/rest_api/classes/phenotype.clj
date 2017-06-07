(ns rest-api.classes.phenotype
  (:require
    ;[rest-api.classes.phenotype.widgets.overview :as overview]
    [rest-api.classes.phenotype.widgets.ontology-browser :as ontology-browser]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "phenotype"
   :widget
   {;:overview overview/widget
    :ontology_browser ontology-browser/widget}})
