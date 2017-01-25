(ns datomic-rest-api.rest.fields.transcript
  (:require 
   [datomic-rest-api.rest.helpers.expression :as expression]
   [datomic.api :as d]))

(defn fpkm-expression-summary-ls 
  "Used for the expression widget."
  [transcript]
  (let [db (d/entity-db transcript)]
    (->>
     (d/q '[:find ?gene .
            :in $ ?transcript
            :where
            [?gene :gene/corresponding-transcript ?th]
            [?th :gene.corresponding-transcript/transcript ?transcript]]
          db (:db/id transcript))
     (d/entity db)
     (expression/fpkm-expression-summary-ls))))
