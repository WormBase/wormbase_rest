(ns rest-api.classes.protein
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.protein.widgets.overview :as overview]
    [rest-api.classes.protein.widgets.location :as location]
    [rest-api.classes.protein.widgets.sequences :as sequences]
    [rest-api.classes.protein.widgets.motif-details :as motif-details]
    [rest-api.classes.protein.widgets.homology :as homology]
    [rest-api.classes.protein.widgets.blast-details :as blast-details]
    [rest-api.classes.protein.widgets.history :as history]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "protein"
   :widget
   {:overview overview/widget
    :location location/widget
    :history history/widget
    :homology homology/widget
    :blast_details blast-details/widget
    :sequences sequences/widget
    :motif_details motif-details/widget
    :external_links external-links/widget}})
