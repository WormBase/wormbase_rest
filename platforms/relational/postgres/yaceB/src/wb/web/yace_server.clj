(ns wb.web.yace-server
  (:use hiccup.core
        ring.middleware.stacktrace)
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as j]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(defn pool [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))

(def pooled-db (delay (pool {:subprotocol "postgresql"
                             :subname "//127.0.0.1/yaceb"
                             :user "postgres"
                             :password "wormbase"})))
(defn db [] @pooled-db)

(def ^:private observed-phenotypes
  "select rnai.display_name as rnai_name, 
          phenotype.display_name as phenotype_name 
     from gene_j_rnai, rnai, rnai_j_phenotype, phenotype 
    where gene_j_rnai.gene = ? 
      and rnai.id = gene_j_rnai.rnai 
      and rnai_j_phenotype.rnai = rnai.id 
      and rnai_j_phenotype.phenotype = phenotype.id
    group by rnai_name, phenotype_name")

(def ^:private not-observed-phenotypes
  "select rnai.display_name as rnai_name, 
          phenotype.display_name as phenotype_name 
     from gene_j_rnai, rnai, rnai_j_notphenotype, phenotype 
    where gene_j_rnai.gene = ? 
      and rnai.id = gene_j_rnai.rnai 
      and rnai_j_notphenotype.rnai = rnai.id 
      and rnai_j_notphenotype.notphenotype = phenotype.id
    group by rnai_name, phenotype_name")

(defn- gene-phenotype-table [db gene pheno-query]
  (let [phenotypes (j/query db [pheno-query gene])
        papers (->> (j/query db ["select rnai.display_name as rnai_name, 
                                         paper.wbpaper_id as paper_id,
                                         author.display_name as first_author
                                  from gene_j_rnai, paper_j_rnai, paper, rnai, paper_j_author, author
                                  where gene_j_rnai.gene = ? 
                                    and paper_j_rnai.rnai = gene_j_rnai.rnai 
                                    and paper.id = paper_j_rnai.paper 
                                    and rnai.id = gene_j_rnai.rnai
                                    and paper_j_author.paper = paper.id
                                    and paper_j_author.sort = 1
                                    and author.id = paper_j_author.author
                                  group by rnai_name, paper_id, first_author"
                                 gene])
                    (group-by :rnai_name))]
    (html
     [:table {:border 1}
      (for [[pheno rnais] (group-by :phenotype_name phenotypes)]
        [:tr
         [:td pheno]
         [:td
          (for [r rnais]
            [:div
             (:rnai_name r)
             (for [rp (papers (:rnai_name r)) :let [p (:paper_id rp)]]
               [:div
                [:a {:href (str "/paper/" p)}
                 (str (:first_author rp) " et al.")]])])]])])))
               

(defn get-gene-phenotypes [id]
  (let [db (db)
        gene (->> (j/query db ["select id from gene where wbgene_id = ?" id])
                  (first)
                  (:id))]
    (html
     [:h3 "Observed phenotypes"]
     (gene-phenotype-table db gene observed-phenotypes)
     [:h3 "Not-observed phenotypes"]
     (gene-phenotype-table db gene not-observed-phenotypes))))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
     (get-gene-phenotypes (:id params))))

(def wb-yace-app (wrap-stacktrace routes))
(defonce server (run-jetty #'wb-yace-app {:join? false
                                          :port 8101}))
