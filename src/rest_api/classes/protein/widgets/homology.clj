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
       hits (some->> (d/q '[:find [?h ...]
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
                      (when-let [score (:locatable/score obj)]

		       {:hit (pack-obj homologous-protein)
                        :method (->> obj :locatable/method :method/id)
                        :evalue (let [evalue-str (format "%7.0e" (/ 1 (math/expt 10 score)))]
                                   (if (= evalue-str "  0e+00")
                                     "      0"
                                     evalue-str))
                        :description (or (:protein/description homologous-protein)
                                       (or
                                        (->> homologous-protein
                                             :cds.corresponding-protein/_protein
                                             :cds/brief-identification)
                                         (when-let [cds-id (->> homologous-protein
                                                                :cds.corresponding-protein/_protein
                                                                  (map (fn [h]
                                                                        (let [cds (:cds/_corresponding-protein h)]
                                                                         (when (not (= "history" (:method/id (:locatable/method cds))))
                                                                          (:cds/id cds)))))
                                                                  (first))]
                                            (str "gene " cds-id))))
                        :percent (if (and (:locatable/min obj) (:locatable/max obj))
                                    (let [hlength (- (:homology/max obj) (:homology/min obj))
                                          percentage (/ hlength plength)]
                                       (format "%.1f" (double (* 100 percentage)))))
		        :taxonomy (if-let [species-id (->> homologous-protein :protein/species :species/id)]
				      (let [[genus species] (str/split species-id #" ")]
				       {:genus (first genus)
				      :species species}))
		        :score (:locatable/score obj)
                        :species (->> homologous-protein :protein/species :species/id)
		      })))))
               (remove nil?)
               (group-by :species)
	       (map (fn [method-group]
	              (let [max-hit (apply max-key :score (second method-group))]
                       (dissoc max-hit :score :species :method)))))]
  {:data {:biggest (:protein/id p)
          :hits hits}
   :description (if hits
                 "best BLASTP hits from selected species"
		 "no homologous proteins found, no best blastp hits to display")}))

(defn homology-image [p]
  {:data "1"
   :description "a dynamically generated image representing homologous regions of the protein"})


(defn- homology-type-to-string [group]
 (if (contains? group :homology-type/cog)
  "COG"
  (if (contains? group :homology-type/eunog)
   "euNOG"
   (if (contains? group :homology-type/fog)
    "FOG"
    (if (contains? group :homology-type/id)
     (:homology-type/id group)
     (if (contains? group :homology-type/kog)
      "KOG"
      (if (contains? group :homology-type/lse)
       "LSE"
       (if (contains? group :homology-type/menog)
	"meNOG"
	(if (contains? group :homology-type/nog)
	 "NOG"
	 (if (contains? group :homology-type/twog)
	  "TWOG"
          ""))))))))))

(defn homology-group [p]
  {:data (some->> (:homology-group/_protein p)
                  (map (fn [g]
                      {:title (first (:homology-group/title g))
		       :type (if (contains? g :homology-group/orthomcl-group)
			      "OrthoMCL_group"
			      (if (contains? g :homology-group/inparanoid_group)
				"InParanoid_group"
                                (let [firstpart
                                  (if (contains? g :homology-group/eggnog-code)
                                    "eggNOG"
                                    (when (contains? g :homology-group/cog-code)
                                    "COG"))
                                      secondpart (homology-type-to-string
                                                   (if (= firstpart "eggNOG")
						    (:homology-group/eggnog-type g)
						    (:homology-group/cog-type g)))]
                                   (str firstpart ":" secondpart))))
                       :id (pack-obj g)})))
   :description "KOG homology groups of the protein"})

(def widget
  {:name generic/name-field
   :best_blastp_matches best-blastp-matches
   :homology_group homology-group
   :homology_image homology-image})
