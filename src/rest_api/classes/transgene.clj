(ns rest-api.classes.transgene
  (:require
    [rest-api.classes.transgene.widgets.phenotypes :as phenotypes]
    [rest-api.classes.transgene.widgets.overview :as overview]
    [rest-api.classes.transgene.widgets.expression :as expression]
    [rest-api.classes.transgene.widgets.human-diseases :as human-diseases]
    [rest-api.classes.transgene.widgets.isolation :as isolation]
    [rest-api.classes.transgene.widgets.tissue-specific-transgenes :as tissue-specific-transgenes]
    [rest-api.classes.transgene.widgets.mapped-transgenes :as mapped-transgenes]
    [rest-api.classes.transgene.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transgene"
   :widget
   {:overview overview/widget
    :phenotypes phenotypes/widget
    :expr_pattern expression/widget
    :human_diseases human-diseases/widget
    :isolation isolation/widget
    :mapped_transgenes mapped-transgenes/widget
    :tissue_specific_transgenes tissue-specific-transgenes/widget
    :references references/widget}})
