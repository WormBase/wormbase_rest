(ns rest-api.classes.go-term.widgets.overview
  (:require
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn type-field [gt]
  {:data (when-let [t (:go-term/type gt)]
           (str/replace (str/capitalize (name t)) #"-" " "))
   :description "type for this term"})

(defn term [gt]
  {:data (pack-obj gt)
   :description "GO term"})

(defn definition [gt]
  {:data (first (:go-term/definition gt))
   :description "term definition"})

(def widget
  {:name generic/name-field
   :type type-field
   :term term
   :definition definition})
