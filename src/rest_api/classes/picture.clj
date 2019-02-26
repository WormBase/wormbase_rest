(ns rest-api.classes.picture
  (:require
    [rest-api.classes.picture.widgets.lightbox :as lightbox]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "picture"
   :widget
   {:lightbox lightbox/widget}})
