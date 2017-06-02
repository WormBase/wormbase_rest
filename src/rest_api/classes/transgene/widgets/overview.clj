(ns rest-api.classes.transgene.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn other-reporter [t]
  {:data (when-let [constructs (:construct/_transgene-construct t)]
           (not-empty
             (remove
               nil?
               (first ; flatten didn't seem to work here - should really use flatten: WBTransgene00000012
                   (for [construct constructs]
                     (:construct/other-reporter construct))))))
   :description "other reporters of this construct"})

(defn utr [t]
  {:data (when-let [constructs (:construct/_transgene-construct t)]
           (not-empty
             (remove
               nil?
               (flatten
                 (for [construct constructs]
                   (when-let [ghs (:construct/three-prime-utr construct)]
                     (for [gh ghs]
                       (pack-obj (:construct.three-prime-utr/gene gh)))))))))
   :description "3' UTR for this transgene"})

(defn used-for [t]
  {:data nil ; very similar to what is in construct class - WBTransgene00000001
   :description "The Transgene is used for"})

(defn marked-rearrangement [t] ; also this one does not work in the Ace version
  {:data (when-let [markers (:transgene/marker-for t)]
           (:transgene.marker-for/text (first markers)))
   :description "rearrangements that the transgene can be used as a marker for"})

(defn fusion-reporter [t]
  {:data (when-let [constructs (:construct/_transgene-construct t)]
           (not-empty
             (remove
               nil?
               (flatten
                 (for [construct constructs]
                   (when-let [frs (:construct/fusion-reporter construct)]
                     (for [fr frs]
                       (obj/tag-obj fr))))))))
   :description "reporter construct for this construct"})

(defn synonym [t]
  {:data (:transgene/synonym t)
   :description "a synonym for the transgene"})

(defn driven-by-gene [t]
  {:data (when-let [constructs (:construct/_transgene-construct t)]
           (not-empty
             (remove
               nil?
               (flatten
                 (for [construct constructs]
                   (when-let [genes (:construct/driven-by-gene construct)]
                     (for [gene genes]
                       (pack-obj (:construct.driven-by-gene/gene gene)))))))))
   :description "gene that drives the transgene"})

(defn purification-tag [t] ; This one is not producing data from Ace
  {:data (when-let [constructs (:construct/_transgene-construct t)]
           (not-empty
             (remove
               nil?
               (flatten
                 (for [construct constructs]
                   (when-let [tags (:construct/purification-tag construct)]
                     (for [tag tags]
                       (obj/tag-obj tag))))))))
   :description "the purification tag for the construct"})

(defn strains [t]
  {:data (when-let [strains (:transgene/strain t)]
           (for [strain strains] (pack-obj strain)))
   :description "Strains associated with this transgene"})

(defn recombination-site [s]
  {:data nil ; no examples available in datomic based on query
   :k (keys s)
   :d (:db/id s)
   :description "map position of the integrated transgene"})

(defn gene-product [t]
  {:data  (when-let [constructs (:construct/_transgene-construct t)]
           (not-empty
             (remove
               nil?
               (flatten
                 (for [construct constructs]
                   (when-let [genes (:construct/gene construct)]
                     (for [gene genes]
                       (pack-obj (:construct.gene/gene gene)))))))))
   :description "gene products for this transgene"})

(def widget
  {:name generic/name-field
   :other_reporter other-reporter
   :utr utr
   :used_for used-for
   :marked_rearrangement marked-rearrangement
   :taxonomy generic/taxonomy
   :fusion_reporter fusion-reporter
   :summary generic/summary
   :synonym synonym
   :driven_by_gene driven-by-gene
   :purification_tag purification-tag
   :strains strains
   :remarks generic/remarks
   :recombination_site recombination-site
   :gene_product gene-product})
