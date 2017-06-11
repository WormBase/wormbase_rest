(ns rest-api.classes.microarray-results.widgets.overview
  (:require
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn cds [mr]
  {:data (when-let [cdshs (:microarray-results/cds mr)]
           (for [cdsh cdshs
                 :let [cds (:microarray-results.cds/cds cdsh)]]
             (pack-obj cds)))
   :description "The corresponding cds"})

(defn gene [mr]
  {:data (when-let [genehs (:microarray-results/gene mr)]
           (for [geneh genehs
                 :let [gene (:microarray-results.gene/gene geneh)]]
             (pack-obj gene)))
   :description "The corresponding genes"})

(defn transcript [mr]
  {:data (when-let [ths (:microarray-results/transcript mr)]
           (for [th ths
                 :let [t (:microarray-results.transcript/transcript th)]]
             (pack-obj t)))
   :description "The corresponding transcripts"})

(defn pseudogene [mr]
  {:data (when-let [pseudogenehs (:microarray-results/pseudogene mr)]
           (for [pseudogeneh pseudogenehs
                 :let [pseudogene (:microarray-results.pseudogene/pseudogene pseudogeneh)]]
             (pack-obj pseudogene)))
   :description "The corresponding pseudogene"})

(def widget
  {:name generic/name-field
   :cds cds
   :gene gene
   :transcript transcript
   :pseudogene pseudogene})
