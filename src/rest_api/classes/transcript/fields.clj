(ns rest-api.classes.transcript.fields
  (:require
   [datomic.api :as d]
   [rest-api.classes.gene.expression :as expression]))

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

