(ns web.core
  (:use hiccup.core
        ring.middleware.stacktrace
        ring.middleware.params
        ring.middleware.keyword-params
        web.edn
        web.anti-forgery
        ring.middleware.gzip
        ring.middleware.session
        ring.middleware.cookies
        web.widgets
        web.colonnade
        web.query
        clojure.walk
        pseudoace.utils
        web.trace
        web.curate)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (defroutes GET POST ANY context wrap-routes)]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer [redirect file-response]]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util     :refer [format-config-uri]]
            [base64-clj.core :as base64]
            [cheshire.core :as json :refer [parse-string]]
            [web.ssl :as ssl]
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
  (GET "/" [] "hello")
  (friend/logout (ANY "/logout" [] (redirect "/")))
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

  (context "/curate" req (friend/authorize #{::user}
                          (if (env :trace-enable-curation-forms)
                            curation-forms
                            (GET "/*" [] "Curation disabled on this server"))))

  (route/files "/" {:root "resources/public"}))

(defroutes api-routes
  (POST "/api/query" {params :params} (if (env :trace-accept-rest-query) (post-query-restful con params))))

(defn wrap-db [handler]
  (fn [request]
    (handler (assoc request :con con :db (db con)))))

(defn- goog-credential-fn [token]
  (if-let [u (entity (db con) [:user/email (:id (:access-token token))])]
    {:identity token
     :email (:user/email u)
     :wbperson (:person/id (:user/wbperson u))
     :roles #{::user}}))

(defn- ssl-credential-fn [{:keys [ssl-client-cert]}]
  (if-let [u (entity (db con) [:user/x500-cn (->> (.getSubjectX500Principal ssl-client-cert)
                                                  (.getName)
                                                  (re-find #"CN=([^,]+)")
                                                  (second))])]
    {:identity ssl-client-cert
     :wbperson (:person/id (:user/wbperson u))
     :roles #{::user}}))

(def client-config {:client-id      (env :trace-oauth2-client-id)
                    :client-secret  (env :trace-oauth2-client-secret)
                    :callback {:domain (or (env :trace-oauth2-redirect-domain)
                                           "http://127.0.0.1:8130")
                               :path "/oauth2callback"}})
                    

(def uri-config
  {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                        :query {:client_id (:client-id client-config)
                               :response_type "code"
                               :redirect_uri (format-config-uri client-config)
                               :scope "email"}}

   :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (format-config-uri client-config)}}})

(defn- flex-decode [s]
  (let [m (mod (count s) 4)
        s (if (> m 0)
            (str s (.substring "====" m))
            s)]
    (base64/decode s)))
    

(defn- goog-token-parse [resp]
  (let [token     (parse-string (:body resp) true)
        id-token  (parse-string (flex-decode (second (str/split (:id_token token) #"\."))) true)]
    {:access_token (:access_token token)
     :id (:email id-token)}))

(def secure-app
  (-> (compojure.core/routes
       (wrap-routes routes wrap-anti-forgery-ssl)
       api-routes)
      (friend/authenticate {:allow-anon? (not (env :trace-require-login))
                            :workflows [(ssl/client-cert-workflow
                                         :credential-fn ssl-credential-fn)
                                        (oauth2/workflow
                                         {:client-config client-config
                                          :uri-config uri-config
                                          :access-token-parsefn goog-token-parse
                                          :credential-fn goog-credential-fn})]})
      wrap-db
      wrap-edn-params-2
      wrap-keyword-params
      wrap-params
      wrap-stacktrace
      wrap-session
      wrap-cookies))
      

(def trace-port (let [p (env :trace-port)]
                  (cond
                   (integer? p)  p
                   (string? p)   (parse-int p)
                   :default      8120)))

(def trace-ssl-port (let [p (env :trace-ssl-port)]
                        (cond
                          (integer? p)   p
                          (string? p)    (parse-int p))))

(def keystore (env :trace-ssl-keystore))
(def keypass  (env :trace-ssl-password))

(defonce server
  (if trace-ssl-port
    (run-jetty #'secure-app {:port trace-port
                             :join? false
                             :ssl-port trace-ssl-port
                             :keystore keystore
                             :key-password keypass
                             :truststore keystore
                             :trust-password keypass
                             :client-auth :want})
    (run-jetty #'secure-app {:port trace-port
                             :join? false})))
