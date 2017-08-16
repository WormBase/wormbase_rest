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

(defn corresponding-all [s]
  {:data nil ; this will take a lot of work. might want to use one function for all widgets
   :k (keys s)
   :d (:db/id s)
   :description "corresponding cds, transcripts, gene for this protein"})

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
   :corresponding_all corresponding-all})
