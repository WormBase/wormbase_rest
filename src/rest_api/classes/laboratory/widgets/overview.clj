(ns rest-api.classes.laboratory.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn website [lab]
  {:data (first (:laboratory/url lab))
   :description "website of the lab"})

(defn representatives [lab]
  {:data (when-let [rs (:laboratory/representative lab)]
           (for [r rs]
             (pack-obj r)))
   :description "official representatives of the laboratory"})

(defn email [lab]
  {:data (first (:laboratory/e-mail lab))
   :description "primary email address for the lab"})

(defn allele-designation [lab]
  {:data (:laboratory/allele-designation lab)
   :description "allele designation of the laboratory"})

(defn affiliation [lab]
  {:data (first (:laboratory/mail lab))
   :description "institute or affiliation of the laboratory"})

(defn strain-designation [lab]
  {:data (:laboratory/id lab) ; gets name in ace code. this is true at least most of the time
   :description "strain designation of the laboratory"})

(def widget
  {:name generic/name-field
   :website website
   :representatives representatives
   :email email
   :allele_designation allele-designation
   :affiliation affiliation
   :remarks generic/remarks
   :strain_designation strain-designation})
