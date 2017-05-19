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
  {:data (when-let [gene (:construct.driven-by-gene/gene
                           (first (:construct/driven-by-gene construct)))]
           (pack-obj gene))
   :description "gene that drives the construct"})

(defn fusion-reporter [construct]
  {:data (when-let [t (:construct/fusion-reporter construct)] (first t))
   :description "reporter construct for this construct"})

(defn gene-product [construct]
  {:data (when-let [ghs (:construct/gene construct)]
           (for [gh ghs :let  [gene (:construct.gene/gene gh)]]
             (pack-obj gene)))
   :description "gene products for this construct"})

(defn other-reporter [construct]
  {:data (when-let [chs  (:construct/other-reporter construct)] (first chs))
   :description (str "The name and WormBase internal ID of " (:construct/id construct))})

(defn remarks [construct]
  {:data (when-let [rhs (:construct/remark construct)]
           (for [rh rhs]
             {:text (:construct.remark/text rh)
              :evidence nil}))
   :description "curatorial remarks for the Construct"})

(defn selection-marker [construct]
  {:data (:construct/selection-marker construct)
   :description "Coinjection marker for this transgene"})

(defn summary [construct]
  {:data  (:construct.summary/text (:construct/summary construct))
   :description (str "a brief summary of the Construct: " (:construct/id construct))})

(defn type-of-construct [construct] ; not finished - need JSON to be fixed for this field
  {:data (:construct/type-of-construct construct)
   :description "type of construct"})

(defn used-for [construct]
  {:data (when-let [tgs (:construct/transgene-construct construct)]
           (for [tg tgs]
             {:use_lab (for [lab (:transgene/laboratory tg)]
                         (pack-obj (:transgene.laboratory/laboratory lab)))
              :use_summary (:transgene.summary/text (:transgene/summary tg))
              :used_in (pack-obj tg)
              :used_in_type (cond
                             (contains? construct :construct/transgene-construct)
                             "Transgene construct")}))
   :description "The Construct is used for"})

(defn utr [construct]
  {:data (when-let [ths (:construct/three-prime-utr construct)]
           (for [th ths :let [gene (:construct.three-prime-utr/gene th)]]
             (pack-obj gene)))
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
