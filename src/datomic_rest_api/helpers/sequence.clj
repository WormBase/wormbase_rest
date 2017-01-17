(ns datomic-rest-api.helpers.sequence
  (:require [hugsql.core :as hugsql]
            [datomic-rest-api.db.sequence :as sequences]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]))

(defn sequence-features [sequence-database gene-id]
 (let [db ((keyword sequence-database) sequences/sequence-dbs)]
   (sequences/gene-features db gene-id)))
