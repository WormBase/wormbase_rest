(ns rest-api.classes.paper.widgets.history
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.classes.paper.core :as paper-generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn history [p]
  {:data (remove
           nil?
           (conj
             []
             (when-let [pmi (:paper/merged-into p)]
               {:action "Merged into"
                :remark (pack-obj pmi)})
             (when-let [pam (:paper/_merged-into p)]
               {:action "Acquires merge"
                :remark (map pack-obj pam)})))
   :description "the curatorial history of the gene"})

(def widget
  {:name generic/name-field
   :history_lite history})
