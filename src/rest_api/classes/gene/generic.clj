(ns rest-api.classes.gene.generic
  (:require
   [rest-api.formatters.object :refer [pack-obj]]))

(defn name-field [gene]
  (let [data (pack-obj "gene" gene)]
    {:data (not-empty data)
     :description (format "The name and WormBase internal ID of %s"
                          (:gene/id gene))}))
