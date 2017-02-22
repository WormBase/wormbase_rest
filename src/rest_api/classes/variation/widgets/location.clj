(ns rest-api.classes.variation.widgets.location
  (:require
    [rest-api.classes.generic :as generic]
    [rest-api.classes.variation.generic :as variation-generic]
    [pseudoace.utils :as pace-utils]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn genetic-position [variation]
  {:data nil
   :description (format "Genetic position of Variation:%s" (:variation/id variation))})

(defn tracks [variation]
  {:data ["GENES"
          "VARIATIONS_CLASSICAL_ALLELES"
          "VARIATIONS_HIGH_THROUGHPUT_ALLELES"
          "VARIATIONS_POLYMORPHISMS"
          "VARIATIONS_CHANGE_OF_FUNCTION_ALLELES"
          "VARIATIONS_CHANGE_OF_FUNCTION_POLYMORPHISMS"
          "VARIATIONS_TRANSPOSON_INSERTION_SITES"
          "VARIATIONS_MILLION_MUTATION_PROJECT"]
   :description "tracks displayed in GBrowse"})

(defn genomic-position [variation]
  {:data nil
   :description "The genomic location of the sequence"})

(defn genomic-image [variation]
  {:data nil
   :description "The genomic location of the sequence to be displayed by GBrowse"})

(def widget
  {:name  variation-generic/name-field
   :genetic_position genetic-position
   :tracks tracks
   :genomic_position genomic-position
   :genomic_image genomic-image})
