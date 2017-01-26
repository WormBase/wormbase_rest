(ns datomic-rest-api.rest.widgets.transcript
  (:require [datomic-rest-api.rest.core :as core]
            [datomic-rest-api.rest.fields.transcript :as transcript-fields]
            [compojure.api.sweet :as sweet]))


(defn routes [db]
  (let [widget-route (partial core/widget-route db "transcript")
        field-route (partial core/field-route db "transcript")]
    (sweet/routes
     (field-route "fpkm_expression_summary_ls" transcript-fields/fpkm-expression-summary-ls))
    ))
