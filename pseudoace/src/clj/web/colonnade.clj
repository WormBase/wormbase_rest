(ns web.colonnade
  (:use hiccup.core
        ring.middleware.anti-forgery)   ;; for *anti-forgery-token*
  (:require [datomic.api :as d :refer (history q touch entity)]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (defroutes GET POST routes)]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(defn- page []
  (html
   [:html
    [:head]
    [:body
     [:div.container-fluid
      [:div#table-maker]]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap.min.css"}]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/css/bootstrap-theme.min.css"}]
     [:link {:rel "stylesheet"
             :href "/css/trace.css"}]
     [:script {:src "http://fb.me/react-0.12.2.js"}]
     [:script {:src "/js/out/goog/base.js"
               :type "text/javascript"}]
     [:script {:src "/js/main.js"
               :type "text/javascript"}]
     [:script {:type "text/javascript"}
      (str "trace_token = '" *anti-forgery-token* "';")
      "goog.require('trace.colonnade');"]]]))


(defn post-query [db params]
  (let [results (if (seq (:rules params))
                  (q (:query params) db (:rules params))
                  (q (:query params) db))]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (pr-str {:query (:query params)
                    :results (take (:max-rows params) (sort-by first results))
                    :count (count results)})}))

(defn colonnade [db]
  (routes
   (GET "/" [] (page))
   (POST "/query" {edn-params :edn-params} (post-query db edn-params))))
