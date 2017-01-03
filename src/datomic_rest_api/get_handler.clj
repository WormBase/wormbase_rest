(ns datomic-rest-api.get-handler
  (:use  pseudoace.utils)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.walk]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (routes GET POST ANY context wrap-routes)]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer (redirect file-response)]
            [cheshire.core :as json :refer (parse-string)]
            [environ.core :refer (env)]
            [hiccup.core :refer (html)]
            [mount.core :as mount]
            [datomic-rest-api.utils.db :refer (datomic-conn)]
            [datomic-rest-api.rest.core :refer (field-adaptor widget-adaptor resolve-endpoint endpoint-urls)]
            [datomic-rest-api.rest.widgets.gene :as gene]))


(declare handle-field-get)
(declare handle-widget-get)

(defn app-routes [db]
  (routes
   (GET "/" []
        (html [:h5 "index"]
              [:ul
               [:li [:a {:href "/rest/field/"} "/rest/field/"]]
               [:li [:a {:href "/rest/widget/"} "/rest/widget/"]]]))
   (GET "/rest/field/" []
        (html [:ul (->> (endpoint-urls "field")
                        (map #(vector :li %)))]))
   (GET "/rest/widget/" []
        (html [:ul (->> (endpoint-urls "widget")
                        (map #(vector :li %)))]))
   (GET "/rest/widget/:schema-name/:id/:widget-name" [schema-name id widget-name :as request]
        (handle-widget-get db schema-name id widget-name request))
   (GET "/rest/field/:schema-name/:id/:field-name" [schema-name id field-name :as request]
        (handle-field-get db schema-name id field-name request))))


(defn init []
  (print "Making Connection\n")
  (mount/start))

(defn app [request]
  (let [db (d/db datomic-conn)
        handler (app-routes db)]
    (handler request)))

(defn- get-port [env-key & {:keys [default]
                            :or {default nil}}]
  (let [p (env env-key)]
    (cond
      (integer? p) p
      (string? p)  (parse-int p)
       :default default)))


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

(defn- handle-widget-get [db schema-name id widget-name request]
  (if-let [widget-fn (resolve-endpoint "widget" schema-name widget-name)]
    (let [adapted-widget-fn (widget-adaptor widget-fn)
          data (adapted-widget-fn db schema-name id)]
      (-> {:name id
           :class schema-name
           :url (:uri request)
           :fields data}
          (json-response)))
    (-> {:message (format "%s widget for %s not exist or not available to public"
                          (str/capitalize widget-name)
                          (str/capitalize schema-name))}
        (json-response)
        (ring.util.response/status 404))))

;; END of REST handler for widgets and fields
