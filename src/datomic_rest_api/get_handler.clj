(ns datomic-rest-api.get-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [compojure.api.sweet :as sweet]
   [datomic-rest-api.db.main :refer [datomic-conn]]
   [datomic-rest-api.rest.widgets.gene :as gene]
   [datomic-rest-api.rest.widgets.transcript :as transcript]
   [datomic.api :as d]
   [hiccup.core :refer [html]]
   [mount.core :as mount]
   [schema.core :as s]))

(defn app-routes [db]
  (sweet/api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :options {:ui {;; validator doesn't work with private url: https://github.com/Orange-OpenSource/angular-swagger-ui/issues/43
                    :validatorUrl nil}}
     :data {:info {:title "WormBase REST API"
                   :description "WormBase REST API"}}}}
   (sweet/routes (gene/routes db)
                 (transcript/routes db))))

(defn init []
  (print "Making Connection\n")
  (mount/start))

(defn app [request]
  (let [db (d/db datomic-conn)
        handler (app-routes db)]
    (handler request)))
