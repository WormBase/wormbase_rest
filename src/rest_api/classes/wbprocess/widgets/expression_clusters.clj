(ns rest-api.classes.wbprocess.widgets.expression-clusters
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn expression-clusters [p]
  {:data (some->> (:wbprocess/expression-cluster p)
                  (map (fn [h]
                         (let [cluster (:wbprocess.expression-cluster/expression-cluster h)]
                           {:id (pack-obj cluster)
                            :evidence {:text (first (:expression-cluster/description cluster))
                                       :evidence (obj/get-evidence h)}}))))
   :description (str "The name and WormBase internal ID of " (:wbprocess/id p))})

(def widget
  {:name generic/name-field
   :expression_clusters expression-clusters})
