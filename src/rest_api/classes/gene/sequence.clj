(ns rest-api.classes.gene.sequence
  (:require
   [rest-api.db.sequence :as wb-seq]))

(defn sequence-features [db-name gene-id]
 (let [db ((keyword db-name) wb-seq/sequence-dbs)]
   (wb-seq/gene-features db gene-id)))
