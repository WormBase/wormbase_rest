(ns rest-api.classes.do-term.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- gene-disease-relevance [gene]
  (when-let [drhs (:gene/disease-relevance gene)]
    (for [drh drhs :let [note (:gene.disease-relevance/note drh)
                         species (:gene.disease-relevance/species drh)]]
      {:text note
       :evidence (obj/get-evidence drh)})))

(defn- gene-disease-orthologs [gene]
  (when-let [ohs (:gene/ortholog gene)]
    (remove
      nil?
      (for [oh ohs :let [sh (:gene.ortholog/species oh)

                         species-id (:species/ncbi-taxonomy sh)]]
        (if (= species-id 9606)
          (let [ortholog (:gene.ortholog/gene oh)
                dhs (:gene/database ortholog)
                id  (first
                      (remove
                        nil?
                        (for [dh dhs
                              :let [source-db (:database/id (:gene.database/database dh))
                                    source-type  (:database-field/id (:gene.database/field dh))]]
                          (if (and
                                (= source-db "OMIM")
                                (= source-type "gene"))
                            (:gene.database/accession dh)))))]
            (if (some? id)
              {:lebel (str "OMIM:" id)
               :class "OMIM"
               :id id})))))))

(defn gene-orthology [d] ; tested on DOID:0050432 - getting more results hear than ace version
  {:error nil
   :data (when-let [gs (:gene.disease-potential-model/_do-term d)]
           (vals
             (apply
               merge
               (for [g gs :let [gene (:gene/_disease-potential-model g)]]
                 {(:gene/id gene)
                  {:gene (pack-obj gene)
                   :relevance (gene-disease-relevance gene)
                   :human_orthologs (gene-disease-orthologs gene)}}))))
   :description "Genes by orthology to human disease gene"})

(defn parent [d]
  {:data (when-let [disease-parents (:do-term/is-a d)]
           (for [parent disease-parents]
             (pack-obj parent)))
   :description "Parent of this disease ontology"})

(defn omim [d]
  {:data (when-let [ids (when-let [dbhs (:do-term/database d)]
                          (not-empty
                            (remove
                              nil?
                              (for [dbh dbhs
                                    :let [database (:do-term.database/database dbh)
                                          id (:do-term.database/accession dbh)]]
                                (if (= (:database/id database) "OMIM") id)))))]
           {:disease {:ids ids}})
   :description "link to OMIM record"})

(defn status [d]
  {:data (when-let [s (:do-term/status d)]
           (name s))
   :description (str "current status of the " (:do-term/id d) " if not Live or Valid")})

(defn child [d]
  {:data (when-let [disease-children (:do-term/_is-a d)]
           (for [child disease-children]
             (pack-obj child)))
   :description "Children of this disease ontology"})

(defn definition [d]
  {:data (:do-term/definition d)
   :description "Definition of this disease"})

(defn genes-biology [d] ; tested on DOID:0050432 - getting more results hear than ace version
  {:error nil
   :data (when-let [gs (:gene.disease-experimental-model/_do-term d)]
           (vals
             (apply
               merge
               (for [g gs :let [gene (:gene/_disease-experimental-model g)]]
                 {(:gene/id gene)
                  {:gene (pack-obj gene)
                   :dbidg (keys g)
                   :relevance (gene-disease-relevance gene)
                   :human_orthologs (gene-disease-orthologs gene)}}))))
   :description "Genes by orthology to human disease gene"})

(defn synonym [d]
  {:data (when-let [shs (:do-term/synonym d)]
           (for [sh shs] (:do-term.synonym/text sh)))
   :description "Synonym of this disease"})

(defn remarks [d]
  {:data (:db/id d)
   :description "curatorial remarks for the DO_term"})

(defn type-field [d]
  {:data (when-let [ts (:do-term/type d)]
           (for [t ts] (str/capitalize (name t))))
   :description "Type of this disease"})

(def widget
  {:gene_orthology gene-orthology
   :parent parent
   :omim omim
   :status status
   :name generic/name-field
   :child child
   :definition definition
   :genes_biology genes-biology
   :synonym synonym
   :remarks remarks
   :type type-field})
