(ns rest-api.classes.do-term
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    ;[rest-api.classes.do-term.widgets.overview :as overview]
    [rest-api.classes.do-term.widgets.ontology-browser :as ontology-browser]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "do-term"
   :uri-name "disease"
   :widget
   {;:overview overview/widget ; two fields are getting more results than perl code
    :ontology_browser ontology-browser/widget
    :external_links external-links/widget}})
