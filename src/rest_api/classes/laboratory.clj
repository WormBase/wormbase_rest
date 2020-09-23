(ns rest-api.classes.laboratory
  (:require
    [rest-api.classes.laboratory.widgets.alleles :as alleles]
    [rest-api.classes.laboratory.widgets.gene-classes :as gene-classes]
    [rest-api.classes.laboratory.widgets.members :as members]
    [rest-api.classes.laboratory.widgets.overview :as overview]
    [rest-api.classes.laboratory.widgets.strains :as strains]
    [rest-api.classes.laboratory.widgets.all-labs :as all-labs]
    [rest-api.classes.graphview.widget :as graphview]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "laboratory"
   :widget
   {:alleles alleles/widget
    :graphview graphview/widget
    :gene_classes gene-classes/widget
    :members members/widget
    :all_labs all-labs/widget
    :overview overview/widget
    :strains strains/widget}})
