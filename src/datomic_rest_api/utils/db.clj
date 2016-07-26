(ns datomic-rest-api.utils.db
  (:require [mount.core :refer [defstate]]
            [environ.core :refer (env)]
            [datomic.api :as d]))

(def uri (env :trace-db))

(defn- new-datomic-connection [uri]
    (d/connect uri))

(defn- datomic-disconnect [conn]
   (d/release conn))

(defstate datomic-conn :start (new-datomic-connection uri)
                       :stop (datomic-disconnect datomic-conn))
