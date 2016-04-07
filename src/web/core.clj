(ns web.core
  (:use hiccup.core
        ring.middleware.stacktrace
        ring.middleware.params
        ring.middleware.keyword-params
        ring.middleware.multipart-params
        web.edn
        web.anti-forgery
        ring.middleware.gzip
        ring.middleware.session
        ring.middleware.cookies
        web.widgets
        web.query
        clojure.walk
        pseudoace.utils)
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
  ;;          pseudoace.object     ;; Not actually used but we want it available   - doesn't work with the latest pseudoace clojure.jar
                          ;; via the REST query mechanism.
            [web.rest.gene :as gene]
            [web.rest.interactions :refer (get-interactions get-interaction-details)]
            [web.rest.references :refer (get-references)]
            [web.locatable-api :refer (feature-api)]))

(def uri (env :trace-db))
(def con (d/connect uri))

(defroutes routes
  (GET "/" [] "hello")
  (friend/logout (ANY "/logout" [] (redirect "/")))

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
       (gene/external-links (db con) (:id params))))

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
      wrap-multipart-params
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
