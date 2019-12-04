(ns rest-api.classes.protein.widgets.blast-details
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [clojure.math.numeric-tower :as math]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn ** [x n] (reduce * (repeat n x)))

(defn blast-details [p]
  {:data (let [db-homology (d/db datomic-homology-conn)
               db (d/db datomic-conn)
               plength (->> p :protein/peptide :protein.peptide/length)]
          (some->> (d/q '[:find [?h ...]
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
                            (when (not (str/starts-with? homologous-protein-id "MSP")) ; skip mass-spec results
                             {:hit (when-let [obj (pack-obj homologous-protein)]
                                    (conj obj {:label (:id obj)}))
                              :taxonomy (let [[genus species] (str/split (->> homologous-protein
                                                                            :protein/species
                                                                            :species/id)
                                                               #" ")]
                                     {:genus (first genus)
                                      :species species})
                              :description (or (:protein/description homologous-protein)
					      (first (:protein/gene-name homologous-protein)))
                              :evalue (when-let [score (:locatable/score obj)]
                                       (let [evalue-str (format "%7.0e" (/ 1 (math/expt 10 score)))]
					(if (= evalue-str "  0e+00")
					 "      0"
					 evalue-str)))
                              :percentage (if (and (:locatable/min obj) (:locatable/max obj))
					      (let [hlength (- (:homology/max obj) (:homology/min obj))
					       percentage (/ hlength plength)]
					       (format "%.1f" (double (* 100 percentage)))))
                              :source_range (if (and (:homology/min obj) (:homology/max obj))
                                             (str (+ 1 (:homology/min obj)) ".." (:homology/max obj)))
                              :target_range (if (and (:locatable/min obj) (:locatable/max obj))
                                             (str (+ 1 (:locatable/min obj)) ".." (:locatable/max obj)))}))))
                    (remove nil?)))
   :description "The Blast details of the protein"})

(def widget
  {:name generic/name-field
   :blastp_details blast-details})
