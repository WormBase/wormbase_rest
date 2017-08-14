(ns rest-api.classes.transcript.widgets.overview
  (:require
  [rest-api.classes.generic-fields :as generic]))

(defn sequence-type [t]
  {:data (cond
           (=  "a" "b")
           "Wormbase Transcript"

           :else
           "unknown"
           )
   :t (:transcript/type t)
   :description "the general type of the sequence"})

(defn feature [t]
  {:data nil ; can't find example
   :description "feature associated with this transcript"})

(def widget
  {:name generic/name-field
;   :available_from generic/available-from
;   :taxonomy generic/taxonomy
   :sequnece_type sequence-type
;   :description generic/description
   :feature feature
;   :identity generic/identity-field
;   :remarks generic/remarks
;   :method generic/method
   :corresponding_all generic/corresponding-all})
