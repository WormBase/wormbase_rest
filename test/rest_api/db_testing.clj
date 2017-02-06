(ns rest-api.db-testing
  (:require
   [datomic.api :as d]
   [mount.core :as mount]
   [rest-api.db.main :refer [datomic-conn]]))

(defn db-lifecycle [f]
  (mount/start)
  (f)
  (mount/stop))

(defn entity [lookup-ref]
  (d/entity (d/db datomic-conn) lookup-ref))

