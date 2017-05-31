(ns rest-api.classes.transgene.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
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

(defn synonym [t]
  {:data nil
   :description "a synonym for the transgene"})

(defn purification-tag [t]
  {:data nil
   :description "the purification tag for the construct"})

(defn strains [t]
  {:data nil
   :description "Strains associated with this transgene"})

(defn recombination-site [s]
  {:data nil
   :description "map position of the integrated transgene"})

(def widget
  {:name generic/name-field
   :other_reporter other-reporter
   :utr utr
   :used_for used-for
   :marked_rearrangement marked-rearrangement
   :taxonomy generic/taxonomy
   :fusion_reporter generic/fusion-reporter
   :summary generic/summary
   :synonym synonym
   :driven_by_gene generic/driven-by-gene
   :purification_tag purification-tag
   :strains strains
   :remarks generic/remarks
   :recombination_site recombination-site
   :gene_product generic/gene-product})
