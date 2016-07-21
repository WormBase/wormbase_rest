(ns datomic-rest-api.resource.system
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defn db-connections 
  (component/system-map
   :datomic-db (start (new-datomic-db (env :db-url)
   :myslq "sdfs")))
