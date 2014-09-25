(ns wb.web.mongo-server
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:require [monger.core :as m]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(def con (m/connect))
(def db (m/get-db con "wormbase"))

(defn- gene-phenotype-table [rnais key]
  (let [phenotypes (->> (mapcat key rnais)
                        (set)
                        (map (fn [pid]
                               (mc/find-one-as-map db "Phenotype" {:_id pid}))))
        papers (->> (mapcat :Reference rnais)
                    (set)
                    (map (fn [pid]
                           (mc/find-one-as-map db "Paper" {:_id pid})))
                    (map (juxt :_id identity))
                    (into {}))]
    (html
     [:div (str "Got " (count papers) " papers")]
     [:table {:border 1}
      (for [pheno phenotypes]
        [:tr
         [:td (first (:Name pheno))]
         [:td
          (for [r rnais
                :when (contains? (set (key r)) (:_id pheno))]
            [:div 
             (:_id r)
             (for [pid (:Reference r)
                   :let [p (papers pid)]]
               [:div
                [:a {:href (str "/paper/" (:_id p))}
                 (first (:Author p)) " et al."]])
             (if-let [strain (:expt_strain r)]
               [:div
                (str "Strain: " strain)])])]])])))

(defn get-gene-phenotypes [id]
  (let [gene (mc/find-one-as-map db "Gene" {:_id (str "Gene~" id)})
        rnais (for [rr (:RNAi_result gene)
                    :let [r (first (keys rr))]]
                (mc/find-one-as-map db "RNAi" {:_id r}))]
    (html
     [:h3 "Observed phenotypes"]
     (gene-phenotype-table rnais :Phenotype)
     [:h3 "Not-observed phenotypes"]
     (gene-phenotype-table rnais :Phenotype_not_observed))))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
    (get-gene-phenotypes (:id params))))

(def wb-mongo-app (wrap-stacktrace routes))
(defonce server (run-jetty #'wb-mongo-app {:join? false
                                           :port 8107}))
