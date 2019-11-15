(ns rest-api.classes.protein.widgets.homology
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [clojure.math.numeric-tower :as math]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn ** [x n] (reduce * (repeat n x)))

(defn best-blastp-matches [p]
 (let [db-homology (d/db datomic-homology-conn)
       db (d/db datomic-conn)
       plength (->> p :protein/peptide :protein.peptide/length)
       data (some->> (d/q '[:find [?h ...]
		            :in $hdb ?pid
			    :where
			     [$hdb ?e :protein/id ?pid]
			     [$hdb ?h :homology/protein ?e]]
			    db-homology
			    (:protein/id p))
	       (map (fn [id]
		     (let [obj (d/entity db-homology id)
		      homologous-protein-id (some->> (:locatable/parent obj)
			      (:protein/id))
		      homologous-protein (some->> (d/q '[:find [?p ...]
				      :in $db ?pid
				      :where
				      [$db ?p :protein/id ?pid]]
				      db
				      homologous-protein-id)
			      (first)
			      (d/entity db))]
		     {:plength plength
  	 	      :hit (pack-obj homologous-protein)
		      :taxonomy (let [[genus species] (str/split (:species/id (:protein/species homologous-protein)) #" ")]
				     {:genus (first genus)
		  		      :species (str " " species)})
 		      :eval  1; (format "%7.3g" (** 10 (* -1 (:locatable/score obj))))
		      :ls (:locatable/score obj)
		     }))))]
  {:data data
   :description (if data
                 "best BLASTP hits from selected species"
		 "no homologous proteins found, no best blastp hits to display")}))

(def widget
  {:name generic/name-field
   :best_blastp_matches best-blastp-matches})
