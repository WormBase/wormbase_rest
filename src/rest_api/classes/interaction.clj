(ns rest-api.classes.interaction
  (:require
    [rest-api.classes.gene.widgets.external-links :as external-links]
    [rest-api.classes.interaction.widgets.overview :as overview]
    [rest-api.classes.interaction.widgets.references :as references]
    [rest-api.classes.interaction.widgets.interactions :as interactions]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "interaction"
   :widget
   {:overview overview/widget
    :graphview graphview/widget
    :interactions interactions/widget
    :references references/widget
    :external_links external-links/widget}
   :field
   {:interaction_details interactions/interaction-details}})
