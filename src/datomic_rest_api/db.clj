(ns datomic-rest-api.db
  (:require [mount.core :refer [defstate]]
            [environ.core :refer (env)]
            [datomic.api :as d]))

(defn- new-datomic-connection []
  (d/connect (env :trace-db)))

(defn- datomic-disconnect [conn]
  (d/release conn))

(defstate datomic-conn
  :start (new-datomic-connection)
  :stop (datomic-disconnect datomic-conn))
