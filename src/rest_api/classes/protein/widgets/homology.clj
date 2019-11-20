(ns rest-api.classes.protein.widgets.homology
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.protein.core :as protein-core]))


(defn ** [x n] (reduce * (repeat n x)))

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
   :homology_groups homology-groups
   :homology_image homology-image})
