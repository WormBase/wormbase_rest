(ns rest-api.classes.construct
  (:require
    [rest-api.classes.construct.widgets.overview :as overview]
    [rest-api.classes.construct.widgets.transgene :as transgene]
    [rest-api.classes.construct.widgets.isolation :as isolation]
    [rest-api.classes.construct.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "construct"
   :widget
   {:overview overview/widget
    :transgene transgene/widget
    :isolation isolation/widget ; construction details widget
    :references references/widget}})
