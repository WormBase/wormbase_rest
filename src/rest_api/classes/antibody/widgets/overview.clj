(ns rest-api.classes.antibody.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn laboratory [antibody]
  {:data (if-let [locations (:antibody/location antibody)]
	   (for [location locations]
	     {:laboratory
              (pack-obj location)

	      :representative
	      (if-let [representatives (:laboratory/representative location)]
		(for [person representatives]
	           (pack-obj person)))}))
   :description "the laboratory where the Antibody was isolated, created, or named"})

(defn corresponding-gene [antibody]
  {:data (if-let [holders (:antibody/gene antibody)]
	  (pack-obj (:antibody.gene/gene (first holders))))
   :description "the corresponding gene the antibody was generated against"})

(defn antigen [antibody]
  {:data {:comment (if-let [antigen (:antibody/antigen antibody)]
                      (:antibody.antigen/text antigen))
          :type (if-let [antigen (:antibody/antigen antibody)]
                    (str/capitalize (name (:antibody.antigen/value antigen))))}
   :description "the type and decsription of antigen this antibody was generated against"})

(defn constructed-by [antibody] ; have not found an example
  {:data (if-let [person (:antibody/person antibody)]
              (pack-obj (first person)))
   :description "the person who constructed the antibody"})

(defn clonality [antibody]
  {:data (if-let [clonality-kw (:antibody/clonality antibody)]
          (str/capitalize (name clonality-kw)))
   :description "the clonality of the antibody"})

(defn summary [antibody]
  {:data (:antibody.summary/text (:antibody/summary antibody))
   :description (str "a brief summary of the Antibody:" (:antibody/id antibody))})

(defn animal [antibody]
  {:data (if-let [animal-value (:antibody.animal/value (:antibody/animal antibody))]
            (str/capitalize (name animal-value)))
   :description "the animal the antibody was generated in"})

(defn remarks [antibody]
  {:data (if-let [remarks (:antibody/remark antibody)]
            (for [remark remarks]
                {:test (:antibody.remark/text remark)
                 :evidence nil}))
   :description "curatorial remarks for the Antibody"})

(defn historical-gene [antibody] ; have not found an example
  {:data (if-let [gene (:antibody/historical-gene antibody)]
                   (pack-obj (first gene)))
   :description "Historical record of the dead genes originally associated with this antibody"})

(defn other-names [antibody]
  {:data (if-let [other-names (:antibody/other-name antibody)]
	   (for [other-name other-names]
	     other-name))
   :description (str "other names that have been used to refer to " (:antibody/id antibody))})


(def widget
  {:laboratory laboratory
   :name generic/name-field
   :corresponding_gene corresponding-gene
   :antigen antigen
   :constructed_by constructed-by
   :clonality clonality
   :summary summary
   :animal animal
   :remarks remarks
   :historical_gene historical-gene
   :other_names other-names})
