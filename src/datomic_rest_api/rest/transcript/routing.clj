(ns datomic-rest-api.rest.transcript.routing
  (:require
   [datomic-rest-api.routes :as routes]
   [datomic-rest-api.rest.transcript.fields :as ts]))

(routes/defroutes-spec
  {:datatype "transcript"
   :field
   {:fpkm_expression_summary_ls ts/fpkm-expression-summary-ls}})
