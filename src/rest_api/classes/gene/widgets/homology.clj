(ns rest-api.classes.gene.widgets.homology
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [rest-api.classes.protein.core :as protein-core]))


(defn ** [x n] (reduce * (repeat n x)))


(defn paralogs [g]
 {:data nil
  :description "precalculated paralog assignments"})

(defn best-blastp-matches [g]
 (let [longest-protein (some->> (:gene/corresponding-cds g)
                                (map :gene.corresponding-cds/cds)
                                (map :cds/corresponding-protein)
                                (map :cds.corresponding-protein/protein)
                                (map (fn [p]
				      {:length (->> p
				                    :protein/peptide
					            :protein.peptide/length)
				       :obj p}))
                                (apply max-key :length))
       hits (protein-core/get-best-blastp-matches (:obj longest-protein))]
  {:data {:biggest (->> longest-protein :obj :protein/id)
          :hits hits}
   :description (if hits
                 "best BLASTP hits from selected species"
		 "no homologous proteins found, no best blastp hits to display")}))

(defn- get-orthologs [g want-nematode]
 (when-let [species-id (->> g :gene/species :species/id)]
             (some->> (:gene/ortholog g)
	              (filter (fn [oh]
			       (let [is-nematode (contains? (:gene.ortholog/gene oh) :gene/corresponding-cds)] ;; only create and connect genes to cds objects if it is a nematode
				(if want-nematode
				 is-nematode
				 (not is-nematode)))))
		      (map (fn [oh]
			    {:ortholog (->> oh :gene.ortholog/gene pack-obj)
                             :method (some->> (:evidence/from-analysis oh)
                                              (map :analysis/id))
                             :species (when-let [species-id (->> oh :gene.ortholog/species :species/id)]
                                         (let [[genus species] (str/split species-id #" ")]
                                          {:genus (first genus)
                                           :species species}))})))))

(defn nematode-orthologs [g]
  {:data (get-orthologs g true)
   :description "precalculated ortholog assignments for this gene"})

(defn other-orthologs [g]
  {:data (get-orthologs g false)
   :description "orthologs of this gene to other species outside of core nematodes at WormBase"})

(defn protein-domains [g]
  {:data nil
   :description "protein domains of the gene"})

(defn treefam [g]
  {:data (some->> (:gene/database g)
                  (map (fn [dh]
                     (when (= (->> dh :gene.database/database :database/id) "TREEFAM")
                        (:gene.database/accession dh))))
                  (remove nil?))
   :description "data and IDs related to rendering Treefam trees"})

(def widget
  {:name generic/name-field
   :paralogs paralogs
   :best_blastp_matches best-blastp-matches
   :nematode_orthologs nematode-orthologs
   :other_orthologs other-orthologs
   :protein_domains protein-domains
   :treefam treefam})
