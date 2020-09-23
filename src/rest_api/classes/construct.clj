(ns rest-api.classes.construct
  (:require
    [rest-api.classes.construct.widgets.overview :as overview]
    [rest-api.classes.construct.widgets.transgene :as transgene]
    [rest-api.classes.construct.widgets.expression :as expression]
    [rest-api.classes.construct.widgets.isolation :as isolation]
    [rest-api.classes.construct.widgets.references :as references]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "construct"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :transgene transgene/widget
    :expr_pattern expression/widget
    :isolation isolation/widget ; construction details widget
    :references references/widget}})
