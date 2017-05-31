(ns rest-api.classes.transposon-family.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn title [t]
  {:data (:transposon-family/title t)
   :descripton "The title of this transposon family"})

(defn description [t]
  {:data (first (:transposon-family/description t))
   :description (str "description of the Transposon_family " (:transposon-family/id t))})

(def widget
  {:name generic/name-field
   :remarks generic/remarks
   :title title
   :description description})
