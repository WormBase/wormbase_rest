(ns rest-api.classes.protein.widgets.motif-details
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn motif-details [p]
  {:data (keys p)
   :d (:db/id p)
   :description "The motif details of the protein"})

(def widget
  {:name generic/name-field
   :motif_details motif-details})
