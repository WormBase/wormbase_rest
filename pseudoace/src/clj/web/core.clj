(ns web.core
  (:use hiccup.core
        ring.middleware.stacktrace
        ring.middleware.params
        web.edn
        ring.middleware.gzip
        ring.middleware.session
        ring.middleware.anti-forgery
        web.widgets
        web.colonnade
        web.query
        clojure.walk
        pseudoace.utils
        web.trace)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (defroutes GET POST context wrap-routes)]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer [file-response]]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [environ.core :refer (env)]
            wb.object     ;; Not actually used but we want it available
                          ;; via the REST query mechanism.
            [web.rest.gene :as gene]
            [web.rest.interactions :refer (get-interactions get-interaction-details)]
            [web.rest.references :refer (get-references)]
            [web.locatable-api :refer (feature-api)]))

(def uri (or (env :trace-db) "datomic:free://localhost:4334/wb248-imp1"))
(def con (d/connect uri))


(def ^:private rules 
  '[[[gene-name ?g ?n] [?g :gene/public-name ?n]]
    [[gene-name ?g ?n] [?c :gene.cgc-name/text ?n] [?g :gene/cgc-name ?c]]
    [[gene-name ?g ?n] [?g :gene/molecular-name ?n]]
    [[gene-name ?g ?n] [?g :gene/sequence-name ?n]]
    [[gene-name ?g ?n] [?o :gene.other-name/text ?n] [?g :gene/other-name ?o]]])

(defn get-gene-by-name [name]
  (let [ddb   (db con)
        genes (q '[:find ?gid
                   :in $ % ?name
                   :where (gene-name ?g ?name)
                          [?g :gene/id ?gid]]
                 ddb rules name)
        oldmems (q '[:find ?gcid
                     :in $ ?name
                     :where [?gc :gene-class/old-member ?name]
                            [?gc :gene-class/id ?gcid]]
                   ddb name)]
    (html
     [:h1 "Matches for " name]
     [:ul
      (for [[gid] genes]
        [:li
         [:a {:href (str "/view/gene/" gid)} gid]])]
     (when-let [o (seq oldmems)]
       [:div
        [:h1 "Old member of..."]
        [:ul
         (for [[gcid] o]
           [:a {:href (str "/view/gene-class/" gcid)} gcid])]]))))


(defn parse-int-if [s]
  (if s
    (Integer/parseInt s)))
        


(defroutes routes
  (GET "/raw2/:class/:id" {params :params db :db}
       (get-raw-obj2
        db
        (:class params)
        (:id params)
        (parse-int-if (params "max-out"))
        (parse-int-if (params "max-in"))
        (= (params "txns") "true")))
  (GET "/attr2/:entid/:attrns/:attrname" {params :params}
       (get-raw-attr2
        (db con)
        (Long/parseLong (:entid params))
        (str (:attrns params) "/" (:attrname params))
        (= (params "txns") "true")))
  (GET "/txns" {params :params}
       (get-raw-txns2
        (db con)
        (let [ids (params "id")]
          (if (string? ids)
            [(Long/parseLong ids)]
            (map #(Long/parseLong %) ids)))))
  (GET "/history2/:entid/:attrns/:attrname" {params :params}
       (get-raw-history2
        (db con)
        (Long/parseLong (:entid params))
        (keyword (.substring (:attrns params) 1) (:attrname params))))
  (GET "/ent/:id" {params :params db :db}
       (get-raw-ent db (Long/parseLong (:id params))))
  (GET "/view/:class/:id" req (viewer-page req))
  (GET "/gene-by-name/:name" {params :params}
       (get-gene-by-name (:name params)))
  
  (GET "/gene-phenotypes/:id" {params :params}
       (gene-phenotypes-widget (db con) (:id params)))
  (GET "/gene-genetics/:id" {params :params}
       (gene-genetics-widget (db con) (:id params)))

  (GET "/rest/widget/gene/:id/overview" {params :params}
       (gene/overview (db con) (:id params)))
  (GET "/rest/widget/gene/:id/history" {params :params}
       (gene/history (db con) (:id params)))
  (GET "/rest/widget/gene/:id/phenotype" {params :params}
       (gene/phenotypes (db con) (:id params)))
  (GET "/rest/widget/gene/:id/interactions" {params :params}
       (get-interactions "gene" (db con) (:id params)))
  (GET "/rest/widget/gene/:id/interaction_details" {params :params}
       (get-interaction-details "gene" (db con) (:id params)))
  (GET "/rest/widget/gene/:id/mapping_data" {params :params}
       (gene/mapping-data (db con) (:id params)))
  (GET "/rest/widget/gene/:id/human_diseases" {params :params}
       (gene/human-diseases (db con) (:id params)))
  (GET "/rest/widget/gene/:id/references" {params :params}
       (get-references "gene" (db con) (:id params)))
  (GET "/rest/widget/gene/:id/reagents" {params :params}
       (gene/reagents (db con) (:id params)))
  (GET "/rest/widget/gene/:id/gene_ontology" {params :params}
       (gene/gene-ontology (db con) (:id params)))
  (GET "/rest/widget/gene/:id/expression" {params :params}
       (gene/expression (db con) (:id params)))
  (GET "/rest/widget/gene/:id/homology" {params :params}
       (gene/homology (db con) (:id params)))
  (GET "/rest/widget/gene/:id/sequences" {params :params}
       (gene/sequences (db con) (:id params)))
  (GET "/rest/widget/gene/:id/feature" {params :params}
       (gene/features (db con) (:id params)))
  (GET "/rest/widget/gene/:id/genetics" {params :params}
       (gene/genetics (db con) (:id params)))
  (GET "/rest/widget/gene/:id/external_links" {params :params}
       (gene/external-links (db con) (:id params)))

  (context "/features" [] feature-api)
  
  (GET "/prefix-search" {params :params}
       (get-prefix-search (db con) (params "class") (params "prefix")))
  (GET "/schema" {db :db} (get-schema db))
  (GET "/rest/auth" [] "hello")

  (POST "/transact" req
        (friend/authorize #{::user}
          (transact req)))
  (context "/colonnade" req (friend/authorize #{::user}
                              (colonnade (db con))))
                                       
  (route/files "/" {:root "resources/public"}))

(defroutes api-routes
  (POST "/api/query" {params :params} (if (env :trace-accept-rest-query) (post-query-restful con params))))

(defn users [username]
  (if-let [u (entity (db con) [:user/name username])]
    {:username (:user/name u)
     :password (:user/bcrypt-passwd u)
     :wbperson (->> (:user/wbperson u)
                    (:person/id))
     :roles    #{::user}}))

(defn wrap-db [handler]
  (fn [request]
    (handler (assoc request :con con :db (db con)))))

(def secure-app
  (-> (compojure.core/routes
       (wrap-routes routes wrap-anti-forgery)
       api-routes)
      (friend/authenticate {:allow-anon? true
                            :unauthenticated-handler #(workflows/http-basic-deny "Demo" %)
                            :workflows [(workflows/http-basic
                                         :credential-fn (partial creds/bcrypt-credential-fn users)
                                         :realm "Demo")]})
      wrap-db
      wrap-edn-params-2
      wrap-params
      wrap-stacktrace
      wrap-session
      wrap-gzip))
      

(def trace-port (let [p (env :trace-port)]
                  (cond
                   (integer? p)  p
                   (string? p)   (parse-int p)
                   :default      8120)))

(defonce server (run-jetty #'secure-app {:port trace-port
                                         :join? false}))
