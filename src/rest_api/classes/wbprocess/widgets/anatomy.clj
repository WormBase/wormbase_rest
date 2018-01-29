(ns rest-api.classes.wbprocess.widgets.anatomy
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn anatomy-term [p]
  {:data (some->> (:wbprocess/anatomy-term p)
                  (map (fn [h]
                         (let [at (:wbprocess.anatomy-term/anatomy-term h)]
                           {:anatomy_term (let [text (pack-obj at)]
                                            (if-let [ev (obj/get-evidence h)]
                                              {:text text
                                               :evidence (obj/get-evidence h)}
                                              text))
                          :description (:anatomy-term.definition/text
                                         (:anatomy-term/definition at))}))))
   :description "Anatomy terms related to this topic"})

(def widget
  {:name generic/name-field
   :anatomy_term anatomy-term})
