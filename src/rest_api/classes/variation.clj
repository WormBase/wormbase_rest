(ns rest-api.classes.variation
  (:require
    [rest-api.classes.variation.widgets.overview :as overview]
    [rest-api.classes.variation.widgets.genetics :as genetics]
    [rest-api.classes.variation.widgets.isolation :as isolation]
    [rest-api.classes.variation.widgets.molecular-details :as molecular-details]
    [rest-api.classes.variation.widgets.phenotypes :as phenotypes]
    [rest-api.classes.variation.widgets.location :as location]
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.gene.widgets.references :as references]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "variation"
   :widget
   {:overview overview/widget
    :genetics genetics/widget
    :isolation isolation/widget
    :molecular_details molecular-details/widget
    :phenotypes phenotypes/widget
    :external_links external-links/widget
    :location location/widget
    :references references/widget}})
