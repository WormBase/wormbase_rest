(ns rest-api.classes.transcript.widgets.overview
  (:require
    [rest-api.formatters.object :as obj :refer [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn feature [t]
  {:data (some->> (:feature.associated-with-transcript/_transcript t)
                  (map :feature/_associated-with-transcript)
                  (map pack-obj)
                  (map (fn [o]
                         {(:label o) o}))
                  (into {}))
   :description "feature associated with this transcript"})

(def widget
  {:name generic/name-field
   :available_from generic/available-from
   :laboratory generic/laboratory
   :taxonomy generic/taxonomy
   :sequnece_type generic/sequence-type
   :description generic/description
   :feature feature
   :identity generic/identity-field
   :remarks generic/remarks
   :method generic/method
   :corresponding_all generic/corresponding-all})
