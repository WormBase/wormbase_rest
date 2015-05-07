(ns pseudoace.scripts.evidence-counts
  (:require [datomic.api :as d :refer (q)]))

(defn evidence-counts
  "Audit use of evidence within evidence-holders scoped under `ns`."
  [db ns]
  (let [evidence (q '[:find [?ei ...]
                      :where [_ :db.install/attribute ?attr]
                             [?attr :db/ident ?ei]
                             [(ground "evidence") ?evns]
                             [(namespace ?ei) ?evns]]
                    db)]
    (for [attr (q '[:find [?ai ...]
                    :in $ ?ns
                    :where [?attr :pace/use-ns "evidence"]
                           [?attr :db/ident ?ai]
                           [(namespace ?ai) ?ns]]
                  db ns)]
      {:attribute attr
       :total (or (q '[:find (count ?h) .
                       :in $ ?attr
                       :where [_ ?attr ?h]]
                     db attr)
                  0)
       :evidence (q '[:find ?ei (count ?h)
                      :in $ ?attr [?ei ...]
                      :where [?ev :db/ident ?ei]
                             [_ ?attr ?h]
                             [?h ?ev _]]
                    db attr evidence)})))

(defn print-evidence-counts [db ns]
  (println)
  (println "### Evidence for" ns)
  (println)
  (doseq [{:keys [attribute total evidence]} (evidence-counts db ns)]
    (println (format "%s (%d)" attribute total))
    (doseq [[ev cnt] evidence]
      (println ev cnt))
    (println)))
