(ns rest-api.classes.expression-cluster.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn algorithm [ec]
  {:data (first (:expression-cluster/algorithm ec))
   :description "Algorithm used to determine cluster"})

(defn taxonomy [ec]
  {:data (if-let [species (:species/id (:expression-cluster/species ec))]
           (let [[genus species] (str/split species #" ")]
             {:genus genus
              :species species}))
   :description "the genus and species of the current object"})

(defn remarks [ec]
  {:data (when-let [rhs (:expression-cluster/remark ec)]
             (for [rh rhs]
                {:text (:expression-cluster.remark/text rh)
                 :evidence nil})) ; have not found example of filled in
   :description "curatorial remarks for the Expression_cluster"})

(defn attribute-of [ec]
  {:data (when-let [mes (:expression-cluster/microarray-experiment ec)]
           (for [me mes ] (pack-obj me)))
   :description "Items attributed to this expression cluster"})

(defn description [ec]
  {:data (first (:expression-cluster/description ec))
   :description (str "description of the Expression_cluster " (:expression-cluster/id ec))})

(def widget
  {:name generic/name-field
   :algorithm algorithm
   :taxonomy taxonomy
   :remarks remarks
   :attribute_of attribute-of
   :description description})
