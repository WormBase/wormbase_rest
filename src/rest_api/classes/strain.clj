(ns rest-api.classes.strain
  (:require
    [rest-api.classes.strain.widgets.phenotypes :as phenotypes]
    [rest-api.classes.strain.widgets.overview :as overview]
    [rest-api.classes.strain.widgets.natural-isolates :as natural-isolates]
    [rest-api.classes.strain.widgets.contains :as contains]
    [rest-api.classes.strain.widgets.human-diseases :as human-diseases]
    [rest-api.classes.strain.widgets.origin :as origin]
    [rest-api.classes.strain.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "strain"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :contains contains/widget
    :human_diseases human-diseases/widget
    :origin origin/widget
    :natural_isolates natural-isolates/widget
    :phenotypes phenotypes/widget
    :references references/widget}})
