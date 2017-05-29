(ns rest-api.classes.transcript.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn available-from [t]
  {:data nil
   :description "availability of clones of the sequence"})

(defn sequence-type [t]
  {:data nil
   :description "the general type of the sequence"})

(defn feature [t]
  {:data nil
   :description "feature associated with this transcript"})

(defn identity-field [t]
  {:data nil
   :description "Brief description of the WormBase transcript"})

(defn method [t]
  {:data nil
   :description "the method used to describe the Transcript"})

(defn corresponding-all [s]
  {:data nil
   :description "corresponding cds, transcripts, gene for this protein"})

(def widget
  {:name generic/name-field
   :laboratory generic/laboratory
   :available_from available-from
   :taxonomy generic/taxonomy
   :sequnece_type sequence-type
   :description generic/description
   :feature feature
   :identity identity-field
   :remarks generic/remarks
   :method method
   :corresponding_all corresponding-all})
