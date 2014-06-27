(ns web.server
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:require [datomic.api :as d :refer (db q)]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(def uri "datomic:free://localhost:4334/smallace")
(def conn (d/connect uri))

(def ^:private pheno-by-id
  '[:find ?p 
    :in $ ?x
    :where [?p :phenotype/id ?x]])

(def ^:private gene-by-id
  '[:find ?g
    :in $ ?gid
    :where [?g :gene/id ?gid]])

(def ^:private paper-by-id
  '[:find ?p
    :in $ ?pid
    :where [?p :paper/id ?pid]])

(defn get-phenotype [id]
  (let [ddb  (db conn)
        pids (q pheno-by-id ddb id)]
    (if (seq pids)
      (let [pheno  (d/touch (d/entity ddb (ffirst pids)))
            rnais  (:rnai/_phenotype pheno)
            nrnais (:rnai/_not.phenotype pheno)]
        (html [:p (str pheno)]
              [:p "rnais " (d/touch (first rnais))]
              [:p "not_rnais " (count (:rnai/_not.phenotype pheno))]))
      (str "Couldn't find " id))))

(defn get-gene-overview [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html 
         [:table
          [:tr [:th "ID"] [:td (:gene/id gene)]]
          (if-let [n (:gene/name.public gene)]
            [:tr [:th "Public name"] [:td n]])
          (if-let [n (:gene/name.sequence gene)]
            [:tr [:th "Sequence name"] [:td n]])
          (if-let [n (:gene/name.cgc gene)]
            [:tr [:th "CGC name"] [:td n]])]))

         ; (for [rlink (:gene/rnai gene)
         ;      :let [r (:gene.rnai/rnai rlink)]]
         ;  [:p (str (d/touch r))])))   
      (str "Couldn't find " id))))

(defn- paper-first-author [paper]
  (->> (:paper/author paper)
       (sort-by :paper.author/ordinal)
       (first)
       (:paper.author/name)))

(defn- gene-phenotype-table [gene key]
  (html
   [:table {:border 1}
    (for [[pheno rnais] (group-by first
                          (mapcat #(for [p (key (:gene.rnai/rnai %))] 
                                     [p %])
                                  (:gene/rnai gene)))]
      [:tr
       [:td (:phenotype/name pheno)]
       [:td (for [[_ rl] rnais
                  :let [r (:gene.rnai/rnai rl)]]
              [:div 
               (:rnai/id r)
               (for [p (:rnai/reference r)]
                 [:div
                  [:a {:href (str "/paper/" (:paper/id p))}
                   (paper-first-author p) " et al."]])
               (if-let [strain (:rnai/expt.strain r)]
                 [:div
                  (str "Strain: " strain)])])]])]))

(defn get-gene-phenotypes [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html
         [:h3 "Observed phenotypes"]
         (gene-phenotype-table gene :rnai/phenotype)
         [:h3 "Not-observed phenotypes"]
         (gene-phenotype-table gene :rnai/not.phenotype)))
      (str "Couldn't find " id))))

(defn get-gene-refs [id]
  (let [ddb  (db conn)
        gids (q gene-by-id ddb id)]
    (if (seq gids)
      (let [gene (d/entity ddb (ffirst gids))]
        (html 
         [:table {:border 1}
          (for [p (:gene/reference gene)]
            [:tr
             [:td (:paper/brief.citation p)]])]))
      (str "Couldn't find " id))))

(defn get-paper [id]
  (let [ddb  (db conn)
        pids (q paper-by-id ddb id)]
    (if (seq pids)
      (let [paper (d/entity ddb (ffirst pids))]
        (html
         (:longtext/text (:paper/abstract paper))))
      (str "Couldn't find " id))))

(defroutes routes
  (GET "/phenotype/:id" {params :params}
    (get-phenotype (:id params)))
  (GET "/gene-overview/:id" {params :params}
    (get-gene-overview (:id params)))
  (GET "/gene-phenotypes/:id" {params :params}
    (get-gene-phenotypes (:id params)))
  (GET "/gene-refs/:id" {params :params}
    (get-gene-refs (:id params)))
  (GET "/paper/:id" {params :params}
    (get-paper (:id params))))

(def appn (wrap-stacktrace routes))
