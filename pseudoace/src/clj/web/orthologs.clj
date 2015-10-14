(ns web.orthologs
  (:use hiccup.core
        ring.middleware.stacktrace
        ring.middleware.params)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(def uri "datomic:free://localhost:4334/biggerace")
(def con (d/connect uri))

(defn get-orthologs []
  (html
   [:h1 "Ortholog fetcher"]
   [:form
    {:action ""
     :method "POST"}
    [:select {:name "species"}
     [:option {:value "Homo sapiens"} "Homo sapiens"]
     [:option {:value "Mus musculus"} "Mus musculus"]]
    [:br]
    [:textarea {:name "genes" :rows 30 :cols 25}]
    [:br]
    [:input {:type "submit"}]]))

(def ^:private rules 
  '[[[gene-name ?g ?n] [?g :gene/name.public ?n]]
    [[gene-name ?g ?n] [?g :gene/name.cgc ?n]]
    [[gene-name ?g ?n] [?g :gene/name.molecular ?n]]])

(defn query-orthologs [db species genes]
  (q '[:find ?gn ?ensg 
       :in $ % ?sn [?gn ...] 
       :where (gene-name ?g ?gn)
              [?s :species/id ?sn]
              [?g :gene/ortholog-other ?go]
              [?go :evidence/link ?p] 
              [?p :protein/species ?s]
              [?p :protein/db-info ?pi] 
              [?pi :db-info/field 
                [:database-field/id "ENSEMBL_geneID"]] 
              [?pi :db-info/accession ?ensg]] 
     db rules species genes))

(defn post-orthologs [species gene-str]
  (html
   [:h1 "Some orthologs"]
   [:table
    {:border 1}
    (for [[gene ortho] (query-orthologs 
                        (db con)
                        species
                        (re-seq #"\S+" gene-str))]
      [:tr
       [:td gene]
       [:td ortho]])]))
    
                                      

(defroutes routes
  (GET "/orthologs" []
    (get-orthologs))
  (POST "/orthologs" {params :params}
    (post-orthologs (params "species") (params "genes"))))
;     (str "params: " params)))


(def app (wrap-stacktrace (wrap-params routes)))

(defonce server (run-jetty #'app {:join? false :port 8110}))
