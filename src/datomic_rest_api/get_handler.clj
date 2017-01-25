(ns datomic-rest-api.get-handler
  (:require
   [cheshire.core :as json :refer (parse-string)]
   [clojure.string :as str]
   ;;[compojure.core :refer (routes GET POST ANY context wrap-routes)]
   [compojure.api.sweet :as sweet :refer (GET)]
   [schema.core :as s]
   [datomic-rest-api.db.main :refer (datomic-conn)]
   ;; widget definition file are required for their "side-effect", ie. register with whitelist
;;   [datomic-rest-api.rest.core :refer (field-adaptor widget-adaptor resolve-endpoint endpoint-urls)]
   [datomic.api :as d :refer (db history q touch entity)]
   [hiccup.core :refer (html)]
   [mount.core :as mount]
   [datomic-rest-api.rest.widgets.gene :refer (gene-routes)]))


(defn app-routes [db]
  (sweet/api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :options {:ui {;; validator doesn't work with private url: https://github.com/Orange-OpenSource/angular-swagger-ui/issues/43
                    :validatorUrl nil}}
     :data {:info {:title "Simple"
                   :description "Compojure Api example"}
            :tags [{:name "api", :description "some apis"}
                   {:name "widget", :description "some widget"}
                   {:name "field", :description "some field"}]}}}
   (apply sweet/routes
          [
           ;; (GET "/rest/widget/:schema-name/:id/:widget-name" [schema-name id widget-name]
           ;;      :tags ["widget"]
           ;;     (handle-widget-get db schema-name id widget-name))
           (gene-routes db)]
       )))

(defn init []
  (print "Making Connection\n")
  (mount/start))

(defn app [request]
  (let [db (d/db datomic-conn)
        handler (app-routes db)]
    (handler request)))
