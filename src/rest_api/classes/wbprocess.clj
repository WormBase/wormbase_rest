(ns rest-api.classes.wbprocess
  (:require
    [rest-api.classes.wbprocess.widgets.phenotypes :as phenotypes]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "wbprocess"
   :widget
   {:phenotypes phenotypes/widget}})
