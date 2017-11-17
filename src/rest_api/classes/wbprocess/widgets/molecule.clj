(ns rest-api.classes.wbprocess.widgets.molecule
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn molecules [p]
  {:data (some->> (:wbprocess/molecule p)
                  (map
                    (fn [h]
                      (let [molecule (pack-obj (:wbprocess.molecule/molecule h))]
                        {:molecule molecule
                         :label (:label molecule)
                         :evidence (obj/get-evidence h)})))
                  (sort-by :label))
   :description "Molecules related to this topic"})

(def widget
  {:name generic/name-field
   :molecules molecules})
