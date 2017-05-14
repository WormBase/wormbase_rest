(ns rest-api.classes.antibody.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn laboratory [antibody]
  {:data nil
   :description "the laboratory where the Antibody was isolated, created, or named"})

(defn corresponding-gene [antibody]
  {:data nil
   :description "the corresponding gene the antibody was generated against"})

(defn antigen [antibody]
  {:data {:comment nil
          :type nil}
   :description "the type and decsription of antigen this antibody was generated against"})

(defn constructed-by [antibody]
  {:data nil
   :description "the person who constructed the antibody"})

(defn clonality [antibody]
  {:data nil
   :description "the clonality of the antibody"})

(defn summary [antibody]
  {:data nil
   :description (str "a brief summary of the Antibody:" (:antibody/id antibody))})

(defn animal [antibody]
  {:data nil
   :description "the animal the antibody was generated in"})

(defn remarks [antibody]
  {:data nil
   :description "curatorial remarks for the Antibody"})

(defn historical-gene [antibody]
  {:data nil
   :description "Historical record of the dead genes originally associated with this antibody"})

(defn other-names [antibody]
  {:data (keys antibody)
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
