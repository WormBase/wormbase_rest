(ns wb.web.titan-server
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:require [clojurewerkz.titanium.graph :as tg]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.edges :as te]
            [clojurewerkz.ogre.core :as q]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.string :as str]))

(def graph (tg/open "titan-cassandra.properties"))

(defn- gene-phenotype-table [rnais key]
  (let [phenotypes (->> (q/query (set rnais)
                                 (q/--> [key])
                                 q/path
                                 q/all-into-vecs!)
                        (group-by second))
        papers     (->> (q/query (set rnais)
                                 (q/--> [:PaperRNAi])
                                 q/path
                                 q/all-into-vecs!)
                        (group-by first))]
    (html
     [:table {:border 1}
      (for [[pheno pheno-rnais] phenotypes]
        [:tr
         [:td (tv/get pheno :primary_name)]
         [:td
          (for [[r _] pheno-rnais]
            [:div
             (tv/get r :name)
             (for [[_ p] (papers r)]
               [:div
                [:a {:href (str "/paper/" (tv/get p :name))}
                 (str (first (str/split (tv/get p :author) #",")) " et al.")]])])]])])))

(defn get-gene-phenotypes [gid]
  (let [gene (first (tv/find-by-kv graph :name gid))
        rnais (q/query #{gene}
                       (q/--> [:RNAiGene])
                       (q/into-vec!))]
    (html
     [:h3 "Observed phenotypes"]
     (gene-phenotype-table rnais :PhenotypeRNAi)
     [:h3 "Not-observed phenotypes"]
     (gene-phenotype-table rnais :PhenotypeNot_in_RNAi))))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
       (get-gene-phenotypes (:id params))))

(def wb-titan-app (wrap-stacktrace routes))
(defonce server (run-jetty #'wb-titan-app {:join? false
                                           :port 8109}))
