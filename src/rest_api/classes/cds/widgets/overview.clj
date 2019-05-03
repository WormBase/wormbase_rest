(ns rest-api.classes.cds.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.classes.generic-functions :as generic-functions]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn description [cds]
  {:data (:cds.detailed-description/text (first (:cds/detailed-description cds)))
   :description (str "description of the CDS " (:cds/id cds))})

(defn partial-field [cds]
  {:data (cond
           (and
             (contains? cds :cds/start-not-found)
             (contains? cds :cds/end-not-found))
           "start and end not found"

           (contains? cds :cds/start-not-found)
           "start not found"

           (contains? cds :cds/end-not-found)
           "end not found"

           :else
           nil)
   :description "Whether the start or end of the CDS is found"})

(def widget
  {:name generic/name-field
   :taxonomy generic/taxonomy
   :sequence_type generic/sequence-type
   :description description
   :partial partial-field
   :identity generic/identity-field
   :remarks generic/remarks
   :method generic/method
   :corresponding_all generic/corresponding-all})
