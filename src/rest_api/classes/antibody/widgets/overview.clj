(ns rest-api.classes.antibody.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

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

(defn constructed-by [antibody] ; no examples in datomic
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

(defn historical-gene [antibody]
  {:data (if-let [ghs (:antibody/historical-gene antibody)]
            (for [gh ghs]
                {:text (pack-obj (:antibody.historical-gene/gene gh))
                 :evidence (when-let [text (:antibody.historical-gene/text gh)]
                             {text ""})}))
   :description "Historical record of the dead genes originally associated with this antibody"})

(def widget
  {:laboratory generic/laboratory
   :name generic/name-field
   :corresponding_gene corresponding-gene
   :antigen antigen
   :constructed_by constructed-by
   :clonality clonality
   :summary summary
   :animal animal
   :remarks generic/remarks
   :historical_gene historical-gene
   :other_names generic/other-names})
