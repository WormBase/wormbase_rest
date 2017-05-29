(ns rest-api.classes.transgene.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn other-reporter [t]
  {:data nil
   :description "other reporters of this construct"})

(defn utr [t]
  {:data nil
   :description "3' UTR for this transgene"})

(defn used-for [t]
  {:data nil
   :description "The Transgene is used for"})

(defn marked-rearrangement [t]
  {:data nil
   :description "rearrangements that the transgene can be used as a marker for"})

(defn fusion-reporter [t]
  {:data nil
   :description "reporter construct for this construct"})

(defn summary [t]
  {:data nil
   :description (str "a brief summary of the Transgene: " (:transgene/id t))})

(defn synonym [t]
  {:data nil
   :description "a synonym for the transgene"})

(defn driven-by-gene [t]
  {:data nil
   :description "gene that drives the transgene"})

(defn purification-tag [t]
  {:data nil
   :description "the purification tag for the construct"})

(defn strains [t]
  {:data nil
   :description "Strains associated with this transgene"})

(defn recombination-site [s]
  {:data nil
   :description "map position of the integrated transgene"})

(defn gene-product [s]
  {:data nil
   :description "gene products for this transgene"})

(def widget
  {:name generic/name-field
   :other_reporter other-reporter
   :utr utr
   :used_for used-for
   :marked_rearrangement marked-rearrangement
   :taxonomy generic/taxonomy
   :fusion_reporter fusion-reporter
   :summary summary
   :synonym synonym
   :driven_by_gene driven-by-gene
   :purification_tag purification-tag
   :strains strains
   :remarks generic/remarks
   :recombination_site recombination-site
   :gene_product gene-product})
