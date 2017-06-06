(ns rest-api.classes.cds
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    ;[rest-api.classes.cds.widgets.overview :as overview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "cds"
   :widget
   {;:overview overview/widget not complete -  has some calculations that would need to be verified
    :external_links external-links/widget}})
