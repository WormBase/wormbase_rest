(ns rest-api.classes.gene.widgets.human-diseases
  (:require
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- gene-disease-relevance [gene]
  (when-let [drhs (:gene/disease-relevance gene)]
    (for [drh drhs :let [note (:gene.disease-relevance/note drh)
                         species (:gene.disease-relevance/species drh)]]
      {:text note
       :evidence (obj/get-evidence drh)})))

(defn human-disease-relevance [gene]
  {:data (let [descriptions
               (->> (:disease-model-annotation.modeled-by-disease-relevant-gene/_gene gene)
                    (map :disease-model-annotation/_modeled-by-disease-relevant-gene)
                    (reduce (fn [result annotation]
                              (if (:disease-model-annotation/disease-model-description annotation)
                                (conj result {:text (clojure.string/join " " (:disease-model-annotation/disease-model-description annotation))
                                              :evidence {:Curator (map pack-obj (:disease-model-annotation/curator-confirmed annotation))}})
                                result))
                            [])
                    (seq))]
           (or descriptions
               ;; or Ranjana :gene/disease-relevance way will become deprecated
               (->> (:gene/disease-relevance gene)
                    (map (fn [dr]
                           {:text (:gene.disease-relevance/note dr)
                            :evidence (obj/get-evidence dr)}))
                    (seq))))
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

(defn detailed-disease-model [gene]
  {:data (let [db (d/entity-db gene)
               models (->> (d/q '[:find [?e ...]
                              :in $ ?g
                              :where
                              [?gh :disease-model-annotation.modeled-by-disease-relevant-gene/gene ?g]
                              [?e :disease-model-annotation/modeled-by-disease-relevant-gene ?gh]]
                            db (:db/id gene))
                           (map (partial d/entity db)))]
           (->> models
                (map (fn [model]
                       {:disease_term (pack-obj (:disease-model-annotation/disease-term model))
                        :genetic_entity (->> ((some-fn (fn [model]
                                                         (->> (:disease-model-annotation/modeled-by-strain model)
                                                              (map :disease-model-annotation.modeled-by-strain/strain)
                                                              (seq)))
                                                       (fn [model]
                                                         (->> (:disease-model-annotation/modeled-by-variation model)
                                                              (map :disease-model-annotation.modeled-by-variation/variation)
                                                              (seq)))
                                                       (fn [model]
                                                         (->> (:disease-model-annotation/modeled-by-transgene model)
                                                              (map :disease-model-annotation.modeled-by-transgene/transgene)
                                                              (seq))))
                                              model)
                                             (map pack-obj)
                                             (seq))
                        :association_type (obj/humanize-ident (:disease-model-annotation/association-type model))
                        :evidence_code (->> (:disease-model-annotation/evidence-code model)
                                            (map (fn [evidence-code]
                                                   {:text (:go-code/id evidence-code)
                                                    :evidence {:description (:go-code/description evidence-code)}}))
                                            (seq))
                        :experimental_condition (->> [:disease-model-annotation/inducing-chemical
                                                      :disease-model-annotation/inducing-agent]
                                                     (reduce (fn [result attribute]
                                                               (concat result (attribute model)))
                                                             [])
                                                     (map pack-obj)
                                                     (seq))
                        :modifier (->> [:disease-model-annotation/modifier-transgene
                                        :disease-model-annotation/modifier-variation
                                        :disease-model-annotation/modifier-strain
                                        :disease-model-annotation/modifier-gene
                                        :disease-model-annotation/modifier-molecule
                                        :disease-model-annotation/other-modifier]
                                       (reduce (fn [result attribute]
                                                 (concat result (attribute model)))
                                               [])
                                       (map pack-obj)
                                       (seq))
                        :modifier_association_type (obj/humanize-ident (:disease-model-annotation/modifier-association-type model))
                        :reference (pack-obj (:disease-model-annotation/paper-evidence model))}))
                (seq)))
   :description "Detailed disease model"})

(def widget
  {:name generic/name-field
   :human_disease_relevance human-disease-relevance
   :human_diseases human-diseases
   :detailed_disease_model detailed-disease-model
   })
