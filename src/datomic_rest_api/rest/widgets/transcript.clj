(ns datomic-rest-api.rest.widgets.transcript
  (:require [datomic-rest-api.rest.core :refer [def-rest-widget register-independent-field]]
            [datomic-rest-api.rest.fields.transcript :as transcript-fields]))

(register-independent-field "fpkm_expression_summary_ls" transcript-fields/fpkm-expression-summary-ls)
