(ns pseudoace.scripts.attrib-counts
  (:require [datomic.api :as d :refer (q)]))

(defn multi-counts 
  "Audit usage of multi-cardinality attributes in namespace `ns`."
  [db ns]
  (->> (q '[:find ?attr ?ident
            :in $ ?ns
            :where [_ :db.install/attribute ?attr]
                   [?attr :db/ident ?ident]
                   [(namespace ?ident) ?ns]
                   [?attr :db/cardinality :db.cardinality/many]]
          db ns)
       (sort-by first)
       (map second)
       (map
        (fn [attr]
          (let [counts
                (q '[:find ?id (count ?val)
                     :in $ ?attr ?id-attr
                     :where [?obj ?attr ?val]
                            [?obj ?id-attr ?id]]
                    db attr (keyword ns "id"))
                multis (->> (filter #(> (second %) 1) counts)
                            (map first))]
            {:attribute   attr
             :occurrences (count counts)
             :multiples   (count multis)
             :examples    (take 5 (sort multis))})))))
            
