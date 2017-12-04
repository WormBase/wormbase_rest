(ns rest-api.classes.laboratory.widgets.members
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn- get-lineage-data [m]
 ;; supervised-by supervised worked-with
 (let []
  {:name (pack-obj m)
   :k (keys m)
   :d (:db/id m)
   :level nil
   :start_date nil
   :end_date nil
   :duration nil
   }
  ))

(defn current-members [l]
  {:data (some->> (:laboratory/registered-lab-members l)
                  (map get-lineage-data))
   :description "current members of the laboratory"})

(defn former-members [l]
  {:data (some->> (:labloratory/past-lab-members l)
                  (map get-lineage-data))
   :description "former members of the laboratory"})

(def widget
  {:name generic/name-field
   :current_members current-members})
