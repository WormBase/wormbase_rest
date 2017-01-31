(ns rest-api.classes.transcript.routing
  (:require
   [rest-api.routes :as routes]
   [rest-api.classes.transcript.fields :as ts]))

(routes/defroutes-spec
  {:datatype "transcript"
   :field
   {:fpkm_expression_summary_ls ts/fpkm-expression-summary-ls}})
