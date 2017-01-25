(ns datomic-rest-api.helpers.sequence
  (:require
   [datomic-rest-api.db.sequence :as sequences]))

(defn sequence-features [sequence-database gene-id]
 (let [db ((keyword sequence-database) sequences/sequence-dbs)]
   (sequences/gene-features db gene-id)))
