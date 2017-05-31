(ns rest-api.classes.motif.widgets.overview
  (:require
   [clojure.string :as str]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn homologies [m]
  {:data (:db/id m) ; there are homol fields that are missing in datomic : INTERPRO:IPR011114
   :definition "homology data for this motif"})

(defn gene_ontology [m]
  {:data (when-let [gths (:motif/go-term m)]
           (for [gth gths
                 :let [gt (:motif.go-term/go-term gth)]]
             {:go_term (pack-obj gt)
              :definition (first (:go-term/definition gt))
              :evidence (obj/get-evidence gt)}))
   :description "go terms to with which motif is annotated"})

(defn title [m]
  {:data (first (:motif/title m))
   :description "title for the motif"})

(def widget
  {:name generic/name-field
   :homologies homologies
   :gene_ontology gene_ontology
   :title title
   :remarks generic/remarks})
