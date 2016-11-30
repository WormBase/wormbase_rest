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
            [cheshire.core :as json :refer [parse-string]]
            [environ.core :refer (env)]
            [mount.core :as mount]
            [datomic-rest-api.utils.db :refer [datomic-conn]]
            [datomic-rest-api.rest.core :refer [wrap-field wrap-widget]]
            [datomic-rest-api.rest.gene :as gene]
            [datomic-rest-api.rest.interactions :refer (get-interactions get-interaction-details)]
            [datomic-rest-api.rest.references :refer (get-references)]
            [datomic-rest-api.rest.locatable-api :refer (feature-api)]))


(declare handle-field-get)
(declare handle-widget-get)

(defn app-routes [db]
   (routes
     (GET "/" [] "<html>
                    <h5>Widgets</h5>
                    <ul>
                       <li><a href=\"./rest/widget/\">/rest/widget/</a></li>
                    <ul>
                  </html>")
     (GET "/rest/widget/" [] "<html>
                    <ul>
                      <li>/rest/widget/gene/:id/external_links</li>
                      <li>/rest/widget/gene/:id/overview</li>
                      <li>/rest/widget/gene/:id/history</li>
                      <li>/rest/widget/gene/:id/mapping_data</li>
                      <li>/rest/widget/gene/:id/genetics</li>
                    </ul>
                  </html>")
     (GET "/rest/widget/:class/:id/:widget" [class id widget :as request]
          (handle-widget-get db class id widget request))
     (GET "/rest/field/:class/:id/:field" [class id field]
          (handle-field-get db class id field))))


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

(defn- wrap-response [data]
  (-> data
      (json/generate-string {:pretty true})
      (ring.util.response/response)
      (ring.util.response/content-type "application/json")))

;; REST field handler and helper

;; (defn- wrap-field [field-fn]
;;   (fn [db class id]
;;     (let [wbid-field (str class "/id")]
;;       (field-fn (d/entity db [(keyword wbid-field) id])))))

(defn- resolve-endpoint [class endpoint-name whitelist]
  (if-let [fn-name (-> (str/join "/" [class endpoint-name])
                       (str/replace "_" "-")
                       (whitelist))]
    (resolve (symbol (str "datomic-rest-api.rest." fn-name)))))

(def ^{:private true} whitelisted-widgets
  #{"gene/overview"
    "gene/external"
    "gene/genetics"
    "gene/history"
    "gene/mapping-data"})

(def ^{:private true} whitelisted-fields
  #{"gene/alleles-other"
    "gene/polymorphisms"})

(defn- handle-field-get [db class id field-name request]
  (if-let [field-fn (resolve-endpoint class field-name whitelisted-fields)]
    (let [wrapped-field-fn (wrap-field field-fn)
          data (wrapped-field-fn db class id)]
      (-> {:name id
           :class class}
          (assoc (keyword field-name) data)
          (wrap-response)))
    (-> {:message "field not exist or not available to public"}
        (wrap-response)
        (ring.util.response/status 404))))
;; END of REST field


(defn- handle-widget-get [db class id widget-name request]
  (if-let [widget-fn (resolve-endpoint class widget-name whitelisted-widgets)]
    (let [wrapped-widget-fn (wrap-widget widget-fn)
          data (wrapped-widget-fn db class id)]
      (-> {:name id
           :class class
           :url (:uri request)
           :fields data}
          (wrap-response)))
    (-> {:message (format "%s widget for %s not exist or not available to public"
                          (str/capitalize widget-name)
                          (str/capitalize class))}
        (wrap-response)
        (ring.util.response/status 404))))
