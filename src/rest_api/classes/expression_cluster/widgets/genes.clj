(ns rest-api.classes.expression-cluster.widgets.genes
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn genes [ec]
  {:data (when-let [ghs (:expression-cluster/gene ec)]
           (for [gh ghs
                 :let [gene (:expression-cluster.gene/gene gh)]]
             (pack-obj gene)))
   :description (str "The name and WormBase internal ID of " (:expression-cluster/id ec))})

(def widget
  {:name generic/name-field
   :genes genes})
