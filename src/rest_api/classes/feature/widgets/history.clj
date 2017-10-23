(ns rest-api.classes.feature.widgets.history
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]))

(defn history [f]
  {:data (not-empty
           (remove
             nil?
             (flatten
               (conj
                 []
                 (when-let [holder (:feature/merged-into f)]
                   {:action "Merged into"
                    :remark (let [pobj (pack-obj (:feature.merged-into/feature holder))]
                              (if-let [e (obj/get-evidence holder)]
                                {:text pobj
                                 :evidence e}
                                pobj))})
                 (when-let [holders (:feature.merged-into/_feature f)]
                   (for [holder holders]
                     {:action "Acquires merge"
                      :remark (let [pobj (pack-obj (:feature/_merged-into holder))]
                                (if-let [e (obj/get-evidence holder)]
                                  {:text pobj
                                   :evidence e}
                                  pobj))}))
                 (when-let [holders (:feature/deprecated f)]
                   (for [holder holders]
                     {:action "Deprecated"
                      :remarks (if-let [e (obj/get-evidence holder)]
                                 {:text (:feature.deprecated/text holder)
                                  :evidence holder}
                                 (:feature.deprecated/text holder))}))))))
   :description "the curatorial history of the gene"})

(def widget
  {:name generic/name-field
   :history_lite history})
