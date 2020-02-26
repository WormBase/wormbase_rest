(ns rest-api.classes.protein.widgets.overview
  (:require
   [datomic.api :as d]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]])
  (:import
   (org.biojava.nbio.aaproperties PeptideProperties)))

(defn estimated-molecular-weight [p]
  {:data (when-let [mwh (:protein/molecular-weight p)]
      (format "%.1f" (:protein.molecular-weight/float (first mwh))))
   :description "the estimated molecular weight of the protein"})

(defn pfam-graph [p]
  {:data (let [hdb (d/db datomic-homology-conn)
               db (d/db datomic-conn)
               colors ["#2dcf00" "#ff5353" "#e469fe" "#ffa500" "#00ffff" "#86bcff" "#ff7ff0" "#f2ff7f" "#7ff2ff"]]
          (some->> (d/q '[:find ?m ?l
                          :in $ $hdb ?pid
                          :where
                           [$hdb ?hp :protein/id ?pid]
                           [$hdb ?l :locatable/parent ?hp]
                           [$hdb ?l :homology/motif ?hm]
                           [$hdb ?hm :motif/id ?mid]
                           [$ ?m :motif/id ?mid]]
                          db hdb (:protein/id p))
                   (map-indexed (fn [idx ids]
                     (let [motif (d/entity db (first ids))
                           locatable (d/entity hdb (second ids))]
			    {:mdb (some->> (:motif/database motif)
					    (map :motif.database/database)
					    (map :database/id)
					    (first)
                                            (str/lower-case))
			    :colour (nth colors (mod idx (count colors)))
			    :href (str/replace (:motif/id motif) #":" "/")
			    :text (or (some->> (:motif/database motif)
					    (map (fn [mdh]
						  (if (= (->> mdh
							  :motif.database/field
							  :database-field/id)
						       "short_name")
						   (:motif.database/accession mdh))))
					    (remove nil?)
					    (first))
                                      "")
			    :start (+ 1 (:locatable/min locatable))
			    :end (:locatable/max locatable)
			    :length (- (:locatable/max locatable)
				       (:locatable/min locatable))
			    :metadata {:identifier (:motif/id motif)
			    :description (or (first (:motif/title motif))
					    (:motif/id motif))
			    :database (->> motif
					   :motif/database
					   first
					   :motif.database/database
					   :database/id)
	                    :end (:locatable/max locatable)}})))
                   (group-by :mdb)
                   (map (fn [[database regions]]
			 {:source database
                          :value {:regions (some->> regions
						      (remove (fn [region]
							       (= 1 (:length region))))
 				 	              (map (fn [region]
							    (dissoc region :mdb :length)))
                                                      (map (fn [region]
                                                            (conj region
                                                                  {:startStyle "straight"
                                                                   :endStyle "straight"}))))
                                    :markups (some->> regions
						      (remove (fn [region]
							       (< 1 (:length region))))
	 			 	              (map (fn [region]
							    (dissoc region :mdb :end :length)))
                                                      (map (fn [region]
                                                            (conj region
                                                                  {:metadata (dissoc (:metadata region) :end)
                                                                   :headStyle "diamond"}))))

                                    :length (->> p
                                                 :protein/peptide
                                                 :protein.peptide/length)}}))
                    (sort-by :source)
                    (merge)))
                         
   :description "The motif graph of the protein"})

(defn best-human-match [p]
  {:data (let [hdb (d/db datomic-homology-conn)
               db (d/db datomic-conn)]
          (some->>  (d/q '[:find [?h ...]
                   :in $hdb ?pid
                   :where
                    [$hdb ?e :protein/id ?pid]
                    [$hdb ?h :homology/protein ?e]]
                  hdb (:protein/id p))
            (map (fn [id]
                  (let [obj (d/entity hdb id)
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
                    (when-let [score (:locatable/score obj)]
                      (when (= 9606
                               (->> homologous-protein
                                 :protein/species
                                 :species/ncbi-taxonomy))
                       {:description (:protein/description homologous-protein)
                        :hit (pack-obj homologous-protein)
                        :score score
                        :evalue (let [evalue-str (format "%7.0e" (/ 1 (math/expt 10 score)))]
                                      (if (= evalue-str "  0e+00")
                                       "      0"
                                       evalue-str))}))))))
             (remove nil?)
             (sort-by :score)
             (map (fn [match]
                    (dissoc match :score)))
             (last)))
   :description "best human BLASTP hit"})

(defn estimated-isoelectric-point [p]
  {:data (when-let [aa (->> p
			  :protein/peptide
			  :protein.peptide/peptide
			  :peptide/sequence)]
           (format "%.2f" (. PeptideProperties getIsoelectricPoint aa)))
   :description "the estimated isoelectric point of the protein"})

(defn type-field [p]
  {:data (some->> (:cds.corresponding-protein/_protein p)
                  (map :cds/_corresponding-protein)
                  (map :locatable/method)
                  (map :method/id)
                  (first))
   :description "The type of the protein"})

(def widget
  {:name generic/name-field
   :estimated_molecular_weight estimated-molecular-weight
  ; :status generic/status status does not exist on this entity
   :pfam_graph pfam-graph
   :best_human_match best-human-match
   :taxonomy generic/taxonomy
   :description generic/description
   :estimated_isoelectric_point estimated-isoelectric-point
   :remarks generic/remarks
   :type type-field
   :corresponding_all generic/corresponding-all})
