(ns rest-api.classes.person
  (:require
    [rest-api.classes.person.widgets.laboratory :as laboratory]
    [rest-api.classes.person.widgets.lineage :as lineage]
    [rest-api.classes.person.widgets.overview :as overview]
    [rest-api.classes.person.widgets.publications :as publications]
    [rest-api.classes.person.widgets.tracking :as tracking]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "person"
   :widget
   {:laboratory laboratory/widget
    :lineage lineage/widget
    :overview overview/widget
    :graphview graphview/widget
    :publications publications/widget
    :tracking tracking/widget}})
