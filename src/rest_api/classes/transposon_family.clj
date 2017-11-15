(ns rest-api.classes.transposon-family
  (:require
    [rest-api.classes.transposon-family.widgets.overview :as overview]
    [rest-api.classes.transposon-family.widgets.transposons :as transposons]
    [rest-api.classes.transposon-family.widgets.var-motifs :as var-motifs]
    [rest-api.routing :as routing]))

(routing/defroutes
  {:entity-ns "transposon-family"
   :widget
   {:overview overview/widget
    :transposons transposons/widget
    :var_motifs var-motifs/widget}})
