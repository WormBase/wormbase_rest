(ns rest-api.classes.person
  (:require
;;   [rest-api.classes.person.widgets.external-links :as external-links]
   [rest-api.classes.person.widgets.overview :as overview]
   [rest-api.classes.person.widgets.tracking :as tracking]
   [rest-api.classes.person.widgets.laboratory :as laboratory]
   [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "person"
   :widget
   {
    :overview overview/widget
    :tracking tracking/widget
    :laboratory laboratory/widget
   }})




  
