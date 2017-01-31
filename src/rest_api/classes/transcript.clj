(ns rest-api.classes.transcript
  (:require
   [rest-api.routing :as routing]
   [rest-api.classes.gene.expression :as exp]))

(routing/defroutes
  {:datatype "transcript"
   :field
   {:fpkm_expression_summary_ls exp/fpkm-expression-summary-ls}})
