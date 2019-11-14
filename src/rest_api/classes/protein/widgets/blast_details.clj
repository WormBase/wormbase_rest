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
               db (d/db datomic-conn)]
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
                             {;:hit (pack-obj obj)
                             ; :species (keys (:protein/species homologous-protein))
                              :d (:protein/id homologous-protein)
                              :species (let [[genus species] (str/split (->> homologous-protein
                                                                            :protein/species
                                                                            :species/id)
                                                                       #" ")]
                                     {:genus (first genus)
                                      :species species})
                              :ojbk (keys obj)
                             ;:description (keys obj)
                            ; :evalue nil
                              :source_range (if (and (:homology/min obj) (:homology/max obj))
                                             (str (+ 1 (:homology/min obj)) ".." (:homology/max obj)))
                              :target_range (if (and (:locatable/min obj) (:locatable/max obj))
                                             (str (+ 1 (:locatable/min obj)) ".." (:locatable/max obj)))
                            }
			  )))))
	  )
   :description "The Blast details of the protein"})

(def widget
  {:name generic/name-field
   :blast_details blast-details})
