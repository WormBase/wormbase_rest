(ns rest-api.classes.cds.widgets.sequences
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn print-homologies [c]
  {:data nil
   :description "homologous sequences"})

(defn print-blast [c]
  {:data nil
   :description "links to BLAST analyses"})

(def widget
  {:name generic/name-field
   :predicted_exon_structure generic/predicted-exon-structure
   :print_homologies print-homologies
   :print_blast print-blast
   :predicted_unit generic/predicted-units
   :print_sequence generic/print-sequence})
