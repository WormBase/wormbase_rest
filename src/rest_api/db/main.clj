(ns rest-api.db.main
  (:require   
   [datomic.api :as d]
   [environ.core :as environ]
   [mount.core :as mount]))

(defn- connect []
  (let [db-uri (environ/env :trace-db)]
    (d/connect db-uri)))

(defn- disconnect [conn]
  (d/release conn))

(mount/defstate datomic-conn
  :start (connect)
  :stop (disconnect datomic-conn))
