(ns rest-api.classes.wbprocess.widgets.go-term
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn go-term [p]
  {:data (some->> (:wbprocess/go-term p)
                  (map :wbprocess.go-term/go-term)
                  (map (fn [t]
                         {:def (first (:go-term/definition t))
                          :name (pack-obj t)
                          :type (when-let [obj (:go-term/type t)]
                                  (obj/humanize-ident (name obj)))})))
   :description "Gene Ontology Term"})

(def widget
  {:name generic/name-field
   :go_term go-term})
