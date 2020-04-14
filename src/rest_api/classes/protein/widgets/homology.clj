(ns rest-api.classes.protein.widgets.homology
  (:require
   [datomic.api :as d]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [clojure.math.numeric-tower :as math]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.protein.core :as protein-core]))


(defn ** [x n] (reduce * (repeat n x)))

(defn- score-to-evalue [score]
 (let [evalue-str (format "%7.0e" (/ 1 (math/expt 10 score)))]
  (if (= evalue-str "  0e+00")
   "      0"
   evalue-str)))

(defn- remove-introns-from-exons [exons]
 (let [last-stop (atom 0)]
  (flatten
   (for [exon (sort-by :no exons)
    :let [last-stop-position @last-stop
          new-stop-position (+ last-stop-position (:len exon))
          new-start-position (+ 1 last-stop-position)]]
    (do (reset! last-stop new-stop-position)
        {:no (:no exon)
         :min (int (Math/floor (/ new-start-position 3)))
         :max (int (Math/floor (/ new-stop-position 3)))})))))


(defn protein-homology [p]
 {:data (let [hdb (d/db datomic-homology-conn)
              db (d/db datomic-conn)]
         {:match
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
                       locatable (d/entity hdb (second ids))
                       score (:locatable/score locatable)]
                  {:source (pack-obj protein)
                   :species (pack-obj (:protein/species protein))
                   :method (->> locatable :locatable/method :method/id)
                   :score score
                   :evalue (when (some? score) (score-to-evalue score))
                   :min (when-let [lmin (:locatable/min locatable)]
                          (+ 1 lmin))
                   :max (when-let [lmax (:locatable/max locatable)]
                          (+ 1 lmax))}))))

          :motif
          (some->> (protein-core/get-motif-details p)
                   (map (fn [motif]
                          (if (nil? (:score motif))
                           motif
                           (conj motif
                                {:evalue (score-to-evalue (:score motif))})))))

          :peptide
           (when-let [peptide (:protein/peptide p)]
            {:sequence (->> peptide
                            :protein.peptide/peptide
                            :peptide/sequence)
                            :min 1
                            :max (+ 1 (:protein.peptide/length peptide))})

           :exons
           (->> p
                :cds.corresponding-protein/_protein
                first
                :cds/_corresponding-protein
                generic/predicted-exon-structure
                :data
                remove-introns-from-exons)})
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
;   :best_blastp_matches best-blastp-matches
   :protein_homology protein-homology
;   :homology_groups homology-groups
;   :homology_image homology-image
})
