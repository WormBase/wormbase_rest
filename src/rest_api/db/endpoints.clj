(ns rest-api.db.endpoints
  (:require
    [clojure.string :as str]
    [ring.util.http-response :refer :all]
    [rest-api.db.sequence :as db-sequence]
    [compojure.api.sweet :as sweet]
    [rest-api.db.main :refer [datomic-conn]]
    [datomic.api :as d]))

(defn version-handler [request]
  (ok {:data (db-sequence/database-version)}))

(def routes
  [(sweet/GET "/database/version" [] :tags ["database"] version-handler)])
