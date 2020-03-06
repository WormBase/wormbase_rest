(ns rest-api.classes.gene.widgets.homology
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.db.main :refer [datomic-homology-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]
   [rest-api.classes.protein.core :as protein-core]))


(defn ** [x n] (reduce * (repeat n x)))


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
       hits  (when (not (nil? longest-protein))
                 (protein-core/get-best-blastp-matches (:obj longest-protein)))]
  {:data {:biggest (->> longest-protein :obj :protein/id)
          :hits hits}
   :description (if hits
                 "best BLASTP hits from selected species"
		 "no homologous proteins found, no best blastp hits to display")}))

(defn- get-orthologs [homolog-holders want-nematode]
  (let [species-fn
        (fn [oh]
          (or (:gene.ortholog/species oh)
              (:gene.paralog/species oh)))

        gene-fn
        (fn [oh]
          (or (:gene.ortholog/gene oh)
              (:gene.paralog/gene oh)))
        ]

    (some->>
     homolog-holders
     (map (fn [oh]
	    {:ortholog (->> oh
                            (gene-fn)
                            (pack-obj))
             :method (some->> (:evidence/from-analysis oh)
                              (map pack-obj))
             :species (when-let [species-id (->> oh
                                                 (species-fn)
                                                 (:species/id))]
                        (let [[genus species] (str/split species-id #" ")]
                          {:genus (first genus)
                           :species species}))}))
     (seq)))

  )

(defn- is-nematode-ortholog [ortholog-holder]
  (->> ortholog-holder
       :gene.ortholog/gene
       :gene/corresponding-cds)) ;; only create and connect genes to cds objects if it is a nematode


(defn nematode-orthologs [g]
  {:data (let [holders (->> g
                            (:gene/ortholog)
                            (filter is-nematode-ortholog))]
           (get-orthologs holders true))
   :description "precalculated ortholog assignments for this gene"})

(defn other-orthologs [g]
  {:data (let [holders (->> g
                            (:gene/ortholog)
                            (remove is-nematode-ortholog))]
           (get-orthologs holders false))
   :description "orthologs of this gene to other species outside of core nematodes at WormBase"})

(defn paralogs [g]
  {:data (let [holders (:gene/paralog g)]
           (get-orthologs holders false))
   :description "precalculated paralog assignments"})


(defn protein-domains [g]
  {:data (let [db (d/entity-db g)
               hdb (d/db datomic-homology-conn)]
           (some->> (d/q '[:find [?m ...]
                           :in $ $hdb ?gid ?mdid
                           :where
                           [$ ?g :gene/id ?gid]
                           [$ ?g :gene/corresponding-cds ?cdsh]
                           [$ ?cdsh :gene.corresponding-cds/cds ?cds]
                           [$ ?cds :cds/corresponding-protein ?ph]
                           [$ ?ph :cds.corresponding-protein/protein ?p]
                           [$ ?p :protein/id ?pid]
                           [$hdb ?hp :protein/id ?pid]
                           [$hdb ?l :locatable/parent ?hp]
                           [$hdb ?l :locatable/method ?md]
                           [$hdb ?md :method/id ?mdid]
                           [$hdb ?l :homology/motif ?hm]
                           [$hdb ?hm :motif/id ?mid]
                           [$ ?m :motif/id ?mid]
                           ]
                         db hdb (:gene/id g) "interpro")
                    (seq)
                    (map #(d/entity db %))
                    (map pack-obj)
                    (map (fn [packed-motif]
                           [(:label packed-motif) packed-motif]))
                    (into {})))
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
