(ns rest-api.classes.pseudogene
  (:require
    [rest-api.classes.pseudogene.widgets.overview :as overview]
    [rest-api.classes.pseudogene.widgets.feature :as feature]
    [rest-api.classes.pseudogene.widgets.genetics :as genetics]
    [rest-api.classes.pseudogene.widgets.reagents :as reagents]
    [rest-api.classes.pseudogene.widgets.expression :as expression]
    [rest-api.classes.pseudogene.widgets.sequences :as sequences]
    [rest-api.classes.pseudogene.widgets.location :as location]
    [rest-api.classes.gene.expression :as gene-expression]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "pseudogene"
   :widget
   {:overview overview/widget
    :feature feature/widget
    :genetics genetics/widget
    :reagents reagents/widget
    ;:sequences sequences/widget
    :expression expression/widget
    :location location/widget
    }
   :field
   {:fpkm_expression_summary_ls gene-expression/fpkm-expression-summary-ls}})
