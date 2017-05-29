(ns rest-api.classes.transposon-family.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn title [t]
  {:data nil
   :descripton "The title of this transposon family"})

(def widget
  {:name generic/name-field
   :remarks generic/remarks
   :title title
   :description generic/description})
