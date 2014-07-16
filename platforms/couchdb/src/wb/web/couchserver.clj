(ns wb.web.couchserver
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:require [com.ashafa.clutch :as c]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(def db "smallace")

(defn- get-docs [db keys]
  (when (seq keys)
    (map :doc (c/all-documents db
                {:keys         keys
                 :include_docs true}))))

(defn gene-phenotype-table [db rnais key]
  (let [phenotypes (->> (mapcat key rnais)
                        (set)
                        (get-docs db))
        papers (->> (mapcat :reference rnais)
                    (set)
                    (get-docs db)
                    (map (juxt :_id identity))
                    (into {}))]
    (html
     [:table {:border 1}
      (for [pheno phenotypes]
        [:tr
         [:td (:name pheno)]
         [:td
          (for [r rnais
                :when (contains? (set (key r)) (:_id pheno))]
            [:div 
             (:_id r)
             (for [pid (:reference r)
                   :let [p (papers pid)]]
               [:div
                [:a {:href (str "/paper/" (:_id p))}
                 (first (:authors p)) " et al."]])
             (if-let [strain (:expt_strain r)]
               [:div
                (str "Strain: " strain)])])]])])))

(defn get-gene-phenotypes [id]
  (let [gene  (c/get-document db id)
        rnais (get-docs db (->> (:rnai gene)
                                (map :rnai)))]
    (html
     [:h3 "Observed phenotypes"]
     (gene-phenotype-table db rnais :phenotype)
     [:h3 "Not-observed phenotypes"]
     (gene-phenotype-table db rnais :not_phenotype))))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
    (get-gene-phenotypes (:id params))))

(def wb-couch-app (wrap-stacktrace routes))
