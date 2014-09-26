(ns wb.web.orient-server
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:import com.tinkerpop.blueprints.impls.orient.OrientGraph)
  (:require [archimedes.core :as g]
            [archimedes.vertex :as v]
            [archimedes.edge :as e]
            [ogre.core :as q]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.string :as str]))

;;
;; Horrid global state...
;;

(g/set-graph! (OrientGraph. "remote:localhost/smallace"))

(defn values [m]
  (for [[_ v] m]
    v))

(defn- first-author [palist]
  (when-let [fa (->> (map second palist)
                     (sort-by #(v/get % :index))
                     (first))]
    (v/get fa :name)))

(defn- gene-phenotype-table [rnais key]
  (let [phenotypes (->> (q/query (set rnais)
                                 (q/--> [:phenotype-observed])
                                 q/path
                                 q/all-into-vecs!)
                        (group-by second))
        papers     (->> (q/query (set rnais)
                                 (q/--> [:reference])
                                 q/path
                                 q/all-into-vecs!)
                        (group-by first))
        authors    (->> (q/query (set (mapcat #(map second %) (values papers)))
                                 (q/--> [:author])
                                 q/path
                                 q/all-into-vecs!)
                        (group-by first))]
    (html
     [:table {:border 1}
      (for [[pheno pheno-rnais] phenotypes]
        [:tr
         [:td (v/get pheno :name)]
         [:td
          (for [[r _] pheno-rnais]
            [:div
             (v/get r :_id)
             (for [[_ p] (papers r)]
               [:div
                [:a {:href (str "/paper/" (v/get p :_id))}
                 (str (first-author (authors p)) " et al.")]])])]])])))

(defn get-gene-phenotypes [id]
  (let [gene (first (v/find-by-kv :_id id))
        rnais (q/query #{gene}
                       (q/--> [:rnai])
                       (q/into-vec!))]
    (html
     [:h3 "Observed phenotypes"]
     (gene-phenotype-table rnais :phenotype-observed)
     [:h3 "Not-observed phenotypes"]
     (gene-phenotype-table rnais :Phenotype_not_observed))))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
     (get-gene-phenotypes (:id params))))
;  (GET "/gene-phenotypes-so/:id" {params :params}
;       (get-gene-phenotypes-so (:id params))))

(def wb-mongo-app (wrap-stacktrace routes))
(defonce server (run-jetty #'wb-mongo-app {:join? false
                                           :port 8108}))

