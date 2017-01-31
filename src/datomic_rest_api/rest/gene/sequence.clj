(ns datomic-rest-api.rest.gene.sequence
  (:require
   [datomic-rest-api.db.sequence :as wb-seq]))

(defn sequence-features [db-name gene-id]
 (let [db ((keyword db-name) wb-seq/sequence-dbs)]
   (wb-seq/gene-features db gene-id)))
