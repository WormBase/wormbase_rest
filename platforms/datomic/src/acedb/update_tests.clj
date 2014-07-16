(ns acedb.update-tests
  (:require [clojure.string :as str]
            [clojure.contrib.seq-utils :refer (indexed)]
            [datomic.api :as d :refer (q db)]))

(defn update-gene-descriptions [conn n]
  (doseq [[i g] (->> (q '[:find ?g :where [?g :gene/id ?i]] (db conn))
                     (map first)
                     (vec)
                     (shuffle)
                     (take n)
                     (indexed))]
    @(d/transact conn [{:db/id g
                        :gene/desc {:gene.desc/concise (str "iteration " i)
                                    :evidence/automatic "acedb.update-tests/update-gene-descriptions"}}])))

(defn update-gene-descriptions-batch [conn n]
  @(d/transact conn
     (for [[i g] (->> (q '[:find ?g :where [?g :gene/id ?i]] (db conn))
                     (map first)
                     (vec)
                     (shuffle)
                     (take n)
                     (indexed))]
       {:db/id g
        :gene/desc {:gene.desc/concise (str "iteration " i)
                                    :evidence/automatic "acedb.update-tests/update-gene-descriptions-batch"}})))

(defn make-random-rnai [conn prefix n]
  (let [d      (db conn)
        genes  (vec (map first (q '[:find ?g :where [?g :gene/id ?i]] d)))
        phens  (vec (map first (q '[:find ?p :where [?p :phenotype/id ?i]] d)))]
    @(d/transact conn
       (for [i (range n)]
         {:db/id     (rand-nth genes)
          :gene/rnai {:evidence/automatic "acedb.update-tests/make-random-rnai"
                      :gene.rnai/rnai {:rnai/id             (str prefix i)
                                       :rnai/expt.delivery  :rnai.delivery/feeding
                                       :rnai/phenotype      [(rand-nth phens)]}}}))))
