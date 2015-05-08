(ns pseudoace.scripts.evidence-counts
  (:require [datomic.api :as d :refer (q)]))

(defn- evidence-attrs [db]
  (q '[:find [?ei ...]
       :where [_ :db.install/attribute ?attr]
              [?attr :db/ident ?ei]
              [(ground "evidence") ?evns]
              [(namespace ?ei) ?evns]]
     db))

(defn- evidence-holders [db ns]
  (q '[:find [?ai ...]
       :in $ ?ns
       :where [?attr :pace/use-ns "evidence"]
              [?attr :db/ident ?ai]
              [(namespace ?ai) ?ns]]
     db ns))

(defn evidence-counts
  "Audit use of evidence within evidence-holders scoped under `ns`."
  [db ns]
  (let [evidence (evidence-attrs db)]
    (for [attr (evidence-holders db ns)]
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


(defn evidence-counts-lowmem
  "Audit use of evidence within evidence-holders scoped under `ns`.
   Optimize for mimimum peer memory use."
  [db ns]
  (let [evidence (evidence-attrs db)]
    (for [attr (evidence-holders db ns)]
      (->> (d/datoms db :aevt attr)
           (map :v)
           (partition-all 1000)
           (reduce
            (fn [counts entity-slice]
              (->> (q '[:find ?ei (count ?h)
                        :in $ [?h ...] [?ei ...]
                        :where [?ev :db/ident ?ei]
                               [?h ?ev _]]
                      db entity-slice evidence)
                   (reduce
                    (fn [counts [ev ev-cnt]]
                      (update-in counts [:evidence ev] #(+ ev-cnt (or % 0))))
                    (update-in counts [:total] + (count entity-slice)))))
            {:attribute attr :total 0})))))
                                 
(defn print-evidence-counts [db ns]
  (println)
  (println "### Evidence for" ns)
  (println)
  (doseq [{:keys [attribute total evidence]} (evidence-counts-lowmem db ns)]
    (println (format "%s (%d)" attribute total))
    (doseq [[ev cnt] (sort-by first evidence)]
      (println ev cnt))
    (println)))
