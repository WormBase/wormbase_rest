(ns rest-api.classes.expression-cluster.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn algorithm [ec]
  {:data (first (:expression-cluster/algorithm ec))
   :description "Algorithm used to determine cluster"})

(defn attribute-of [ec]
  {:data (when-let [mes (:expression-cluster/microarray-experiment ec)]
           (for [me mes] (pack-obj me)))
   :description "Items attributed to this expression cluster"})

(def widget
  {:name generic/name-field
   :algorithm algorithm
   :taxonomy generic/taxonomy
   :remarks generic/remarks
   :attribute_of attribute-of
   :description generic/description})
