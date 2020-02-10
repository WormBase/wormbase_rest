(ns rest-api.classes.motif.widgets.overview
  (:require
   [datomic.api :as d]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.db.main :refer [datomic-homology-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn homologies [m] ; eg. PFAM:PF00292
 {:data (let [db (d/entity-db m)
              hdb (d/db datomic-homology-conn)]
	 (some->> [(d/q '[:find [?o ...]
                    :in $ $hdb ?mid
                    :where
                     [$hdb ?hm :motif/id ?mid]
                     [$hdb ?h :homology/motif ?hm]
                     [$hdb ?h :locatable/parent ?hp]
                     [$hdb ?hp :protein/id ?hpid]
                     [$ ?o :protein/id ?hpid]]
                  db hdb (:motif/id m))
             (d/q '[:find [?o ...]
                    :in $ $hdb ?mid
                    :where
                     [$hdb ?hm :motif/id ?mid]
                     [$hdb ?h :homology/motif ?hm]
                     [$hdb ?h :locatable/parent ?hp]
                     [$hdb ?hp :dna/id ?hpid]
                     [$ ?o :dna/id ?hpid]]
                  db hdb (:motif/id m))
             (d/q '[:find [?o ...]
                    :in $ $hdb ?mid
                    :where
                     [$hdb ?hm :motif/id ?mid]
                     [$hdb ?h :homology/motif ?hm]
                     [$hdb ?h :locatable/parent ?hp]
                     [$hdb ?hp :motif/id ?hpid]
                     [$ ?o :motif/id ?hpid]]
                  db hdb (:motif/id m))]
            (flatten)
            (distinct)
            (remove nil?)
	    (map (fn [id]
	          (let [obj (d/entity db id)]
		   {:type (cond
		           (contains? obj :protein/id) ; eg. PFAM:PF00292
			   "Peptide"

			   (contains? obj :dna/id)
			   "DNA"

			   (contains? obj :motif/id)
			   "Motif"
                           
                           (contains? obj :other/id); fix this 
                           "Other")
		   :homolog (pack-obj obj)
		   :species (pack-obj
				   (cond
				    (contains? obj :protein/id)
				    (:protein/species obj)

				    (contains? obj :dna/id)
				    (->> obj :sequence/_dna :sequence/species)

				    (contains? obj :motif/id)
				    (->> obj
				     :clone.gel/_motif
				     first
				     :clone/_gel
				     first
				     :clone/species)))})))))
   :definition "homology data for this motif"})

(defn gene_ontology [m]
  {:data (when-let [gths (:motif/go-term m)]
           (for [gth gths
                 :let [gt (:motif.go-term/go-term gth)]]
             {:go_term (pack-obj gt)
              :definition (first (:go-term/definition gt))
              :evidence (obj/get-evidence gt)}))
   :description "go terms to with which motif is annotated"})

(defn title [m]
  {:data (first (:motif/title m))
   :description "title for the motif"})

(def widget
  {:name generic/name-field
   :homologies homologies
   :gene_ontology gene_ontology
   :title title
   :remarks generic/remarks})
