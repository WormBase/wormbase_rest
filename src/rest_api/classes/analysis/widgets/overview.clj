(ns rest-api.classes.analysis.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn conducted-by [analysis]
  {:data nil
   :description "the person that conducted the analysis"})

(defn subproject [analysis]
  {:data nil
   :description "the subproject of the analysis if there is one"})

(defn project [analysis]
  {:data nil
   :description "the project that conducted the analysis"})

(defn description [analysis]
  {:data nil
   :description "description of the Analysis RACE_Vidal_elegans"})

(defn url [analysis]
  {:data nil
   :description "the url of the analysis"})

(defn based-on-wb-release [analysis]
  {:data nil
   :description "the WormBase release the analysis is based on"})

(defn title [analysis]
  {:data nil
   :description "the title of the analysis"})

(defn based-on-db-release [analysis]
  {:data nil
   :description "the database release the analysis is based on"})


(def widget
  {:conducted_by conducted-by
   :subproject subproject
   :project project
   :name generic/name-field
   :description description
   :url url
   :based_on_wb_release based-on-wb-release
   :title title
   :based_on_db_release based-on-db-release})
