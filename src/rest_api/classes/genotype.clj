(ns rest-api.classes.genotype
  (:require
    [rest-api.classes.genotype.widgets.overview :as overview]
    [rest-api.classes.genotype.widgets.human-diseases :as human-diseases]
    [rest-api.classes.genotype.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "genotype"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :human_diseases human-diseases/widget
    :references references/widget}})
