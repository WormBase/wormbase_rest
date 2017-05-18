(ns rest-api.classes.disease.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn gene-orthology [disease]
  {:error nil
   :data nil
   :description "Genes by orthology to human disease gene"})

(defn parent [disease]
  {:data nil
   :description "Parent of this disease ontology"})

(defn omim [disease]
  {:data nil
   :description "link to OMIM record"})

(defn status [disease]
  {:data (keys disease)
   :description (str "current status of the ")}) ;(:disease/id disease) " if not Live or Valid")})

(defn child [disease]
  {:data nil
   :description "Children of this disease ontology"})

(defn definition [disease]
  {:data nil
   :description "Definition of this disease"})

(defn genes-biology [disease]
  {:error nil
   :data nil
   :description "Genes by orthology to human disease gene"})

(defn synonym [disease]
  {:data nil
   :description "Synonym of this disease"})

(defn remarks [disease]
  {:data nil
   :description "curatorial remarks for the DO_term"})

(defn type-field [disease]
  {:data nil
   :description "Type of this disease"})

(def widget
  {:gene_orthology gene-orthology
   :parent parent
   :omim omim
   :status status
 ;  :name generic/name-field
   :child child
   :definition definition
   :genes_biology genes-biology
   :synonym synonym
   :remarks remarks
   :type type-field})
