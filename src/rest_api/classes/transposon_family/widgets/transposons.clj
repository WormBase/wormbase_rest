(ns rest-api.classes.transposon-family.widgets.transposons
  (:require
    [clojure.string :as str]
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn family-members [tf]
  {:data (when-let [transposons (:transposon/_member-of tf)]
           (for [transposon transposons]
             {:copy_status (when-let [status-kw (:transposon.copy-status/status
                                                  (:transposon/copy-status transposon))]
                             (str/capitalize (str (name status-kw))))
              :id (pack-obj transposon)}))
   :description "Transposon members of this family"})

(def widget
  {:name generic/name-field
   :family_members family-members})
