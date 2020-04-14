(ns rest-api.classes.protein.widgets.homology
  (:require
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.protein.core :as protein-core]))


(defn ** [x n] (reduce * (repeat n x)))

(defn protein-homology [p]
 {:data (let [hdb (d/db datomic-homology-conn)
              db (d/db datomic-conn)]
          (some->> (d/q '[:find ?p ?l
                          :in $ $hdb ?pid
                          :where
                           [$hdb ?hp :protein/id ?pid]
                           [$hdb ?l :locatable/parent ?hp]
                           [$hdb ?l :homology/protein ?pr]
                           [$hdb ?pr :protein/id ?hpid]
                           [$ ?p :protein/id ?hpid]
                            ]
                          db hdb (:protein/id p))
           (map (fn [ids]
                 (let [protein (d/entity db (first ids))
                       locatable (d/entity hdb (second ids))]
                  {:source (pack-obj protein)
                   :locatable  {:method (->> locatable :locatable/method :method/id)
                                :min (:locatable/min locatable)
                                :max (:locatable/max locatable)
                                :score (:locatable/score locatable)}
                   :min (:homology/min locatable)
                   :max (:homology/max locatable)})))))
  :description "Homologous proteins for the protein"})

(defn best-blastp-matches [p]
 (let [hits (protein-core/get-best-blastp-matches p)]
  {:data {:biggest (:protein/id p)
          :hits hits}
   :description (if hits
                 "best BLASTP hits from selected species"
		 "no homologous proteins found, no best blastp hits to display")}))


(defn homology-image [p]
  {:data 1
   :description "a dynamically generated image representing homologous regions of the protein"})


(defn homology-groups [p]
  {:data (protein-core/get-homology-groups p)
   :description "KOG homology groups of the protein"})


(def widget
  {:name generic/name-field
   :best_blastp_matches best-blastp-matches
   :protein_homology protein-homology
   :homology_groups homology-groups
   :homology_image homology-image})
