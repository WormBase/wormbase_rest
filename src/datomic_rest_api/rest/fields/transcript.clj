(ns datomic-rest-api.rest.fields.transcript
  (:require [datomic-rest-api.rest.helpers.object :as rest-api-obj :refer (humanize-ident get-evidence author-list pack-obj)]
            [datomic-rest-api.rest.helpers.expression :as expression]
            [datomic.api :as d :refer (db q touch entity)]
            [clojure.string :as str]
            [pseudoace.utils :refer [vmap vmap-if vassoc cond-let those conjv]]))

;;
;; Expression widget
;;
(defn fpkm-expression-summary-ls [transcript]
  (let [db (d/entity-db transcript)]
    (->>
     (q '[:find ?gene .
          :in $ ?transcript
          :where [?gene :gene/corresponding-transcript ?th]
                 [?th :gene.corresponding-transcript/transcript ?transcript]]
        db (:db/id transcript))
     (d/entity db)
     (expression/fpkm-expression-summary-ls))))
