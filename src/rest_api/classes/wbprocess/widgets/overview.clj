(ns rest-api.classes.wbprocess.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn summary [w]
  {:data nil
   :description (str "A brief summary of the WBProcess: " (:wbprocess/id w))})

(defn other-name [w]
  {:data nil
   :description "Term alias"})

(defn life-stage [w]
  {:data nil
   :description "Life stages associated with this topic"})

(defn historical-gene [w]
  {:data nil
   :description "Historical record of the dead genes originally associated with this topic"})

(defn related-process [w]
  {:data nil
   :description "Topics related to this record"})

(def widget
  {:name generic/name-field
   :summary summary
   :other_name other-name
   :life_stage life-stage
   :historical_gene historical-gene
   :related_process related-process})
