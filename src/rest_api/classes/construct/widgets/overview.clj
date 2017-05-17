(ns rest-api.classes.construct.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn construction-summary [construct]
  {:data (first (:construct/construction-summary construct))
   :description "Construction details for the transgene"})

(defn driven-by-gene [construct]
  {:data (when-let [gene (:construct.gene/gene (first (:construct/gene construct)))]
           (pack-obj gene))
   :description "gene that drives the construct"})

(defn fusion-reporter [construct]
  {:data (when-let [t (:construct/fusion-reporter construct)]
           {:class "Text"
            :db "sace://localhost:2005"
            :name (first t)})
   :description "reporter construct for this construct"})

(defn gene-product [construct]
  {:data (:db/id construct)
   :description "gene products for this construct"})

(defn other-reporter [construct]
  {:data nil
   :description (str "The name and WormBase internal ID of " (:construct/id construct))})

(defn remarks [construct]
  {:data nil
   :description "curatorial remarks for the Construct"})

(defn selection-marker [construct]
  {:data nil
   :description "Coinjection marker for this transgene"})

(defn summary [construct]
  {:data  (:construct.summary/text (:construct/summary construct))
   :description (str "a brief summary of the Construct: " (:construct/id construct))})

(defn type-of-construct [construct] ; not finished - need JSON to be fixed for this field
  {:data (when-let [t (:construct/type-of-construct construct)]
           {:class "Text"
            :db "sace://localhost:2005"
            :name (first t)})
   :description "type of construct"})

(defn used-for [construct]
  {:data nil
   :description "The Construct is used for"})

(defn utr [construct]
  {:data nil
   :description "3' UTR for this transgene"})


(def widget
  {:construction_summary construction-summary
   :driven_by_gene driven-by-gene
   :fusion_reporter fusion-reporter
   :gene_product gene-product
   :other_reporter other-reporter
   :remarks remarks
   :selection_marker selection-marker
   :summary summary
   :type_of_construct type-of-construct
   :used_for used-for
   :utr utr
   :name generic/name-field})
