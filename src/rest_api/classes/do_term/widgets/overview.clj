(ns rest-api.classes.do-term.widgets.overview
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.do-term.core :as do-term]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- gene-disease-relevance [gene]
  (some->> (:gene/disease-relevance gene)
           (map (fn [drh]
                  (let [note (:gene.disease-relevance/note drh)]
                    {:text note
                     :evidence (obj/get-evidence drh)})))))

(defn- gene-disease-orthologs [gene]
  (some->> (:gene/ortholog gene)
           (map (fn [oh]
                  (let [sh (:gene.ortholog/species oh)
                        species-id (:species/ncbi-taxonomy sh)]
                    (when (= species-id 9606)
                      (some->> (:gene/database
                                 (:gene.ortholog/gene oh))
                               (map (fn [dh]
                                      (let [source-db (:database/id (:gene.database/database dh))
                                            source-type (:database-field/id (:gene.database/field dh))]
                                        (when (and
                                                (= source-db "OMIM")
                                                (= source-type "gene"))
                                          (let [id (:gene.database/accession dh)]
                                            {:label id
                                             :class "OMIM"
                                             :id id})))))
                               (remove nil?)
                               (first))))))
           (flatten)
           (remove nil?)
           (sort-by :label)))

(defn genes-orthology [d]
  {:error nil
   :data (some->> (:gene.disease-potential-model/_do-term d)
                  (map (fn [g]
                         (let [gene (:gene/_disease-potential-model g)]
                           {(:gene/id gene)
                            {:gene (pack-obj gene)
                             :relevance (gene-disease-relevance gene)
                             :human_orthologs (gene-disease-orthologs gene)}})))
                  (apply merge)
                  (vals))
   :description "Associated genes based on experimental data"})

(defn parent [d]
  {:data (some->> (:do-term/is-a d)
                  (map pack-obj))
   :description "Parent of this disease ontology"})

(defn omim [d]
  {:data (when-let [ids (some->> (:do-term/database d)
                                 (map  (fn [dbh]
                                         (let [database (:do-term.database/database dbh)
                                               id (:do-term.database/accession dbh)]
                                           (when (= (:database/id database) "OMIM") id))))
                                 (remove nil?)
                                 (sort)
                                 (not-empty))]
           {:disease {:ids ids}})
   :description "link to OMIM record"})

(defn child [d]
  {:data (some->> (:do-term/_is-a d)
                  (map pack-obj)
                  (sort-by :label))
   :description "Children of this disease ontology"})

(defn definition [d]
  {:data (:do-term/definition d)
   :description "Definition of this disease"})

(defn genes-biology [d]
  {:error nil
   :data (some->> (:gene.disease-experimental-model/_do-term d)
                  (map :gene/_disease-experimental-model)
                  (map (fn [gene]
                         {(:gene/id gene)
                          {:gene (pack-obj gene)
                           :relevance (gene-disease-relevance gene)
                           :human_orthologs (gene-disease-orthologs gene)}}))
                  (apply merge)
                  (vals))
   :description "Genes by orthology to human disease gene"})

(defn synonym [d]
  {:data (some->> (:do-term/synonym d)
                  (map :do-term.synonym/text)
                  (sort))
   :description "Synonym of this disease"})

(defn type-field [d]
  {:data (some->> (:do-term/type d)
                  (map name)
                  (map obj/humanize-ident)
                  (map (fn [t]
                         (if (= t "Gold") "GOLD" t)))
                  (sort))
   :description "Type of this disease"})

(defn detailed-disease-model [do-term]
  {:data (let [models (:disease-model-annotation/_disease-term do-term)]
           (do-term/process-disease-models models))
   :description "Detailed disease model"})

(def widget
  {:genes_orthology genes-orthology
   :parent parent
   :omim omim
   :status generic/status
   :name generic/name-field
   :child child
   :definition definition
   :genes_biology genes-biology
   :synonym synonym
   :remarks generic/remarks
   :type type-field
   :detailed_disease_model detailed-disease-model})
