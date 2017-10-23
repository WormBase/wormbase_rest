(ns rest-api.classes.phenotype
  (:require
    [rest-api.classes.phenotype.widgets.overview :as overview]
    [rest-api.classes.phenotype.widgets.ontology-browser :as ontology-browser]
    [rest-api.classes.phenotype.widgets.associated-anatomy :as associated-anatomy]
    [rest-api.classes.phenotype.widgets.go-term :as go-term]
    [rest-api.classes.phenotype.widgets.rnai :as rnai]
    [rest-api.classes.phenotype.widgets.transgene :as transgene]
    [rest-api.classes.phenotype.widgets.variation :as variation]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "phenotype"
   :widget
   {:overview overview/widget
    ;:associated_anatomy associated-anatomy/widget
    :go_term go-term/widget
    :rnai rnai/widget
    :transgene transgene/widget
    ;:variation variation/widget
    :ontology_browser ontology-browser/widget}})
