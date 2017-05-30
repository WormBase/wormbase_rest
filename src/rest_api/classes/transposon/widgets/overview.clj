(ns rest-api.classes.transposon.widgets.overview
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [pseudoace.utils :as pace-utils]
   [rest-api.classes.generic :as generic]
   [rest-api.formatters.date :as date]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn copy-status [t]
  {:data (when-let [c (:transposon.copy-status/status
                        (:transposon/copy-status t))]
           (str/capitalize (name c)))
   :description "Copy status of this transposon"})

(defn member-of [t]
  {:data (when-let [m (:transposon/member-of t)]
           (pack-obj (first m)))
   :description "The transposon family this transposon belongs to"})

(defn old-name [t]
  {:data (:transposon.old-name/text (first (:transposon/old-name t)))
   :description "Old name of the transposon"})

(def widget
  {:name generic/name-field
   :copy_status copy-status
   :taxonomy generic/taxonomy
   :remarks generic/remarks
   :member_of member-of
   :old_name old-name})
