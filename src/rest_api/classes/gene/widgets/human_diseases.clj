(ns rest-api.classes.gene.widgets.human-diseases
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- gene-disease-relevance [gene]
  (when-let [drhs (:gene/disease-relevance gene)]
    (for [drh drhs :let [note (:gene.disease-relevance/note drh)
                         species (:gene.disease-relevance/species drh)]]
      {:text note
       :evidence (obj/get-evidence drh)})))

(defn human-disease-relevance [gene]
  {:data (when-let [drs (:gene/disease-relevance gene)]
           (for [dr drs]
             {:text (:gene.disease-relevance/note dr)
              :evidence (obj/get-evidence dr)}))
   :description "curated description of human disease relevance"})

(defn- get-human-diseases [gene field]
  (let [field-kw (keyword "gene" field)
        do-term-kw (keyword (str "gene." field) "do-term")
        species-kw (keyword (str "gene." field) "species")]
  (when-let [ems (field-kw gene)]
    (not-empty
      (remove
        nil?
        (for [em ems
              :let [disease (do-term-kw em)]]
          (when (= 9606 (:species/ncbi-taxonomy (species-kw em)))
            (conj
              {:ev (obj/get-evidence em)}
              (pack-obj disease)))))))))

(defn human-diseases [gene]
  {:data (not-empty
	   (apply
	     merge
	     [
	      (pace-utils/vmap
		:potential_model
		(get-human-diseases gene "disease-potential-model")

		:experimental_model
		(get-human-diseases gene "disease-experimental-model"))
	      (when-let [mapping
			 (remove
			   nil?
			   (when-let [databases (:gene/database gene)]
			     (for [database databases]
			       (when (= (:database/id (:gene.database/database database)) "OMIM")
				 (when-let [id (:gene.database/accession database)]
				   (when-let [field (:database-field/id (:gene.database/field database))]
				     {field id}))))))]
		(into {}
		      (for [k (into #{} (mapcat keys mapping))
			    :let [obj (Object.)]]
			[k (filter (partial not= obj)
				   (map #(get % k obj) mapping))])))]))
   :description "Diseases related to the gene"})

(def widget
  {:name generic/name-field
   :human_disease_relevance human-disease-relevance
   :human_diseases human-diseases})
