(ns rest-api.classes.variation.generic
  (:require
   [rest-api.formatters.object :refer [pack-obj]]))

(defn name-field [variation]
  (let [data (pack-obj "variation" variation)]
    {:data (if (empty? data) nil data)
     :description (format "The name and WormBase internal ID of %s"
                          (:variation/id variation))}))
