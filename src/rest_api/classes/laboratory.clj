(ns rest-api.classes.laboratory
  (:require
    [rest-api.classes.laboratory.widgets.alleles :as alleles]
    [rest-api.classes.laboratory.widgets.gene-classes :as gene-classes]
    [rest-api.classes.laboratory.widgets.overview :as overview]
    ;[rest-api.classes.laboratory.widgets.members :as members]
    [rest-api.classes.laboratory.widgets.strains :as strains]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "laboratory"
   :widget
   {:alleles alleles/widget
    :gene_classes gene-classes/widget
    ;:members members/widget
    :overview overview/widget
    :strains strains/widget}})
