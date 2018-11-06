(ns rest-api.classes.pseudogene.widgets.sequences
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn predicted-exon-structure [p]
  {:data nil
   :description "predicted exon structure within the sequence"})

(defn strand [p]
  {:data nil
   :description "strand orientation of the sequence"})

(def widget
  {:name generic/name-field
;   :predicted_exon_structure predicted-exon-structure
;   :strand strand
   :print_sequence generic/print-sequence})
