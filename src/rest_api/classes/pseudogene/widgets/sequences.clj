(ns rest-api.classes.pseudogene.widgets.sequences
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn strand [p]
  {:data nil
   :description "strand orientation of the sequence"})

(def widget
  {:name generic/name-field
   :predicted_exon_structure generic/predicted-exon-structure
;   :strand strand
   :print_sequence generic/print-sequence})
