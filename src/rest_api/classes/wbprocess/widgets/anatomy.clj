(ns rest-api.classes.wbprocess.widgets.anatomy
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn anatomy-term [p]
  {:data (some->> (:wbprocess/anatomy-term p)
                  (map (fn [h]
                         {:anatomy_term (pack-obj (:wbprocess.anatomy-term/anatomy-term h))
                          :evidence (obj/get-evidence h)})))
   :description "Anatomy terms related to this topic"})

(def widget
  {:name generic/name-field
   :anatomy_term anatomy-term})
