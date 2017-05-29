(ns rest-api.classes.transposon.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn copy-status [t]
  {:data nil
   :description "Copy status of this transposon"})

(defn member-of [t]
  {:data nil
   :description "The transposon family this transposon belongs to"})

(defn old-name [t]
  {:data nil
   :description "Old name of the transposon"})

(def widget
  {:name generic/name-field
   :copy_status copy-status
   :taxonomy generic/taxonomy
   :remarks generic/remarks
   :member_of member-of
   :old_name old-name})
