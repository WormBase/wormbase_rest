(ns rest-api.classes.person
  (:require
   [rest-api.classes.person.widgets.external-links :as external-links]
   [rest-api.classes.person.widgets.overview :as overview]
   [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "person"
   :widget
   {
    :overview overview/widget
   })




  
