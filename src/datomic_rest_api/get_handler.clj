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
            [datomic-rest-api.rest.gene :as gene]
            [datomic-rest-api.rest.interactions :refer (get-interactions get-interaction-details)]
            [datomic-rest-api.rest.references :refer (get-references)]
            [datomic-rest-api.rest.locatable-api :refer (feature-api)]))

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
                    </ul>
                  </html>")
     (GET "/rest/widget/gene/:id/overview" {params :params}
         (gene/overview db (:id params) (str "rest/widget/gene/" (:id params) "/overview")))
     (GET "/rest/widget/gene/:id/history" {params :params}
         (gene/history db (:id params) (str "rest/widget/gene/" (:id params) "/history")))
;;     (GET "/rest/widget/gene/:id/phenotype" {params :params}
;;         (gene/phenotypes db (:id params) (str "rest/widget/gene/" (:id params) "/phenotype"))) ;; broken because of variation/phenotype
;;     (GET "/rest/widget/gene/:id/interactions" {params :params}
;;         (get-interactions "gene" db (:id params) (str "rest/widget/gene/" (:id params) "/interactions"))) ;; needed work on nodes all - not quite lining up
;;     (GET "/rest/field/gene/:id/interaction_details" {params :params}
;;         (get-interaction-details "gene" db (:id params) (str "rest/field/gene/" (:id params) "/interaction_details"))) ;; wormbase is missing data section: why?
     (GET "/rest/widget/gene/:id/mapping_data" {params :params}
         (gene/mapping-data db (:id params) (str "rest/widget/gene/" (:id params) "/mapping_data")))
;;     (GET "/rest/widget/gene/:id/human_diseases" {params :params}
;;         (gene/human-diseases db (:id params) (str "rest/widget/gene/" (:id params) "/human_disease")))
;;     (GET "/rest/widget/gene/:id/references" {params :params}
;;         (get-references "gene" db (:id params) (str "rest/widget/gene/" (:id params) "/references")))
;;     (GET "/rest/widget/gene/:id/reagents" {params :params}
;;         (gene/reagents db (:id params) (str "rest/widget/gene/" (:id params) "/reagents"))) ;; looks correct; needs sort to confirm
;;     (GET "/rest/widget/gene/:id/gene_ontology" {params :params}
;;         (gene/gene-ontology db (:id params) (str "rest/widget/gene/" (:id params) "/gene_ontology"))) ;; substancially same structure. not producing the same results 
;;     (GET "/rest/widget/gene/:id/expression" {params :params}
;;         (gene/expression db (:id params) (str "rest/widget/gene/" (:id params) "/expression"))) ;; This one is predominantly done but needs a little checking and sequence data
;;     (GET "/rest/widget/gene/:id/homology" {params :params}
;;         (gene/homology db (:id params) (str "rest/widget/gene/" (:id params) "/homology"))) ;; need to wait for homology data to be added to datomic database
;;     (GET "/rest/widget/gene/:id/sequences" {params :params}
;;         (gene/sequences db (:id params) (str "rest/widget/gene/" (:id params) "/sequences")))
;;     (GET "/rest/widget/gene/:id/feature" {params :params}
;;         (gene/features db (:id params) (str "rest/widget/gene/" (:id params) "/feature"))) ;; has a few values missing for gbrowse - not sure if needed or possible to get out of datomic
;;     (GET "/rest/widget/gene/:id/genetics" {params :params}
;;         (gene/genetics db (:id params) (str "rest/widget/gene/" (:id params) "/genetics"))) ;; looks good need to find rearrangement that exists. Also need to test if works, when does not exist, with Perl template
     (GET "/rest/widget/gene/:id/external_links" {params :params}
         (gene/external-links db (:id params) (str "rest/widget/gene/" (:id params) "/external_links")))))

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
