(ns datomic-rest-api.utils.db
  (:require [mount.core :refer [defstate]]
            [environ.core :refer (env)]
            [datomic.api :as d]))

(defn- new-datomic-connection [uri]
    (d/connect uri))

(defn- datomic-disconnect [conn]
   (d/release conn))

(defstate datomic-conn
  :start (fn []
           (new-datomic-connection (env :trace-db))
           :stop (datomic-disconnect datomic-conn))
