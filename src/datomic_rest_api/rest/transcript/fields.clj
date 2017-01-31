(ns datomic-rest-api.rest.transcript.fields
  (:require
   [datomic-rest-api.rest.gene.expression :as expression]
   [datomic.api :as d]))

(def q-corresponding-transcript
  '[:find ?gene .
    :in $ ?transcript
    :where
    [?gene :gene/corresponding-transcript ?th]
    [?th :gene.corresponding-transcript/transcript ?transcript]])

(defn fpkm-expression-summary-ls 
  "Used for the expression widget."
  [transcript]
  (let [db (d/entity-db transcript)]
    (->> (d/q q-corresponding-transcript db (:db/id transcript))
         (d/entity db)
         (expression/fpkm-expression-summary-ls))))

