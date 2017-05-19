(ns rest-api.classes.disease
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.disease.widgets.overview :as overview]
    [rest-api.classes.disease.widgets.ontology-browser :as ontology-browser]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "do-term"
   :widget
   {:overview overview/widget
    :ontology_browser ontology-browser/widget
    :external_links external-links/widget}})
