(ns rest-api.classes.variation
  (:require
    [rest-api.classes.variation.widgets.overview :as overview]
    [rest-api.classes.variation.widgets.genetics :as genetics]
    [rest-api.classes.variation.widgets.isolation :as isolation]
    [rest-api.classes.variation.widgets.location :as location]
    [rest-api.classes.variation.widgets.molecular-details :as molecular-details]
    [rest-api.classes.variation.widgets.phenotypes :as phenotypes]
    [rest-api.classes.variation.widgets.external-links :as external-links]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-class "variation"
   :widget
   {:overview overview/widget
    :genetics genetics/widget
    :isolation isolation/widget
    :location location/widget
    :molecular_details molecular-details/widget
    :phenotypes phenotypes/widget
    :external_links external-links/widget}})
