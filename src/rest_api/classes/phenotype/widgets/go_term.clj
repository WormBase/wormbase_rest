(ns rest-api.classes.phenotype.widgets.go-term
  (:require
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [rest-api.classes.generic-fields :as generic]))

(defn go-term [p]
  {:data  (when-let [gths (:phenotype/go-term p)]
            (for [gth gths
                  :let [go-term (:phenotype.go-term/go-term gth)]]
              (pack-obj go-term)))
   :description (str "The name and WormBase internal ID of " (:db/id p))})

(def widget
  {:go_term go-term
   :name generic/name-field})
