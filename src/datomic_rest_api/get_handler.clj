(ns datomic-rest-api.get-handler
  (:require
   [cheshire.core :as json :refer (parse-string)]
   [clojure.string :as str]
   ;;[compojure.core :refer (routes GET POST ANY context wrap-routes)]
   [compojure.api.sweet :as sweet :refer (GET)]
   [schema.core :as s]
   [datomic-rest-api.db.main :refer (datomic-conn)]
   ;; widget definition file are required for their "side-effect", ie. register with whitelist
   [datomic-rest-api.rest.core :refer (field-adaptor widget-adaptor resolve-endpoint endpoint-urls)]
   [datomic.api :as d :refer (db history q touch entity)]
   [hiccup.core :refer (html)]
   [mount.core :as mount]
   [datomic-rest-api.rest.widgets.gene]))

(declare handle-field-get)
(declare handle-widget-get)

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
   (GET "/rest/widget/:schema-name/:id/:widget-name" [schema-name id widget-name]
        :tags ["widget"]
        (handle-widget-get db schema-name id widget-name))
   (GET "/rest/field/:schema-name/:id/:field-name" [schema-name id field-name :as request]
        :tags ["field"]
        ;; :return Pizza
        ;; :body [pizza Pizza]
        :summary "echoes a Pizza"
        (handle-field-get db schema-name id field-name request))))

(defn init []
  (print "Making Connection\n")
  (mount/start))

(defn app [request]
  (let [db (d/db datomic-conn)
        handler (app-routes db)]
    (handler request)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal functions and helper ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; start of REST handler for widgets and fields
(defn- json-response [data]
  (-> data
      (json/generate-string {:pretty true})
      (ring.util.response/response)
      (ring.util.response/content-type "application/json")))

;; start of REST handler for widgets and fields

(defn- handle-field-get [db schema-name id field-name request]
  (if-let [field-fn (resolve-endpoint "field" schema-name field-name)]
    (let [adapted-field-fn (field-adaptor field-fn)
          data (adapted-field-fn db schema-name id)]
      (-> {:name id
           :class schema-name
           :url (:uri request)}
          (assoc (keyword field-name) data)
          (json-response)))
    (-> {:message "field not exist or not available to public "}
        (json-response)
        (ring.util.response/status 404))))

(defn- handle-widget-get [db schema-name id widget-name]
  (if-let [widget-fn (resolve-endpoint "widget" schema-name widget-name)]
    (let [adapted-widget-fn (widget-adaptor widget-fn)
          data (adapted-widget-fn db schema-name id)]
      (-> {:name id
           :class schema-name
           :url (:uri (str/join "/" ["rest" "widget" schema-name id widget-name]))
           :fields data}
          (json-response)))
    (-> {:message (format "%s widget for %s not exist or not available to public"
                          (str/capitalize widget-name)
                          (str/capitalize schema-name))}
        (json-response)
        (ring.util.response/status 404))))

;; END of REST handler for widgets and fields
