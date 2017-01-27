(ns datomic-rest-api.rest.widgets.transcript
  (:require [datomic-rest-api.rest.core :refer [def-rest-routes widget-setting field-setting]]
            [datomic-rest-api.rest.fields.transcript :as transcript-fields]))


(def-rest-routes routes "transcript"
  (field-setting "fpkm_expression_summary_ls" transcript-fields/fpkm-expression-summary-ls))
