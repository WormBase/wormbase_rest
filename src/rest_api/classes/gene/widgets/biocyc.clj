(ns rest-api.classes.gene.widgets.biocyc
  (:require
   [rest-api.classes.generic-fields :as generic]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn pathways [gene]
  (let [gene_id (:gene/id gene)]
  {:data (let [gene_pathways (->> "biocyc.json"
       io/resource
       io/reader
       json/read)]
       (get gene_pathways gene_id))
    :description "The biocyc data for this gene"}
  ))

(def widget
  {:pathways pathways})